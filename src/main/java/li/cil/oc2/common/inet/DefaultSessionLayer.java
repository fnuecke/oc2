package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.*;
import li.cil.oc2.api.inet.layer.SessionLayer;
import li.cil.oc2.api.inet.session.DatagramSession;
import li.cil.oc2.api.inet.session.EchoSession;
import li.cil.oc2.api.inet.session.Session;
import li.cil.oc2.api.inet.session.StreamSession;
import li.cil.oc2.common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Queue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

public final class DefaultSessionLayer implements SessionLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final Executor executor = Executors
        .newSingleThreadExecutor(runnable -> new Thread(runnable, "internet/blocking-session"));

    ///////////////////////////////////////////////////////////////////

    private final AtomicReference<EchoResponse> echoResponse = new AtomicReference<>(null);

    ///////////////////////////////////////////////////////////////////

    private final ReadySessions readySessions = new ReadySessions();

    private final SocketManager socketManager;

    public DefaultSessionLayer(final LayerParameters layerParameters) {
        InternetManager internetManager = layerParameters.getInternetManager();
        socketManager = SocketManager.attach(internetManager);
    }

    @Override
    public void onStop() {
        socketManager.detach();
    }

    @Override
    public void receiveSession(final Receiver receiver) {
        final EchoResponse pending = echoResponse.getAndSet(null);
        if (pending != null) {
            final ByteBuffer data = receiver.receive(pending.session);
            assert data != null;
            data.put(pending.payload);
            data.flip();
            return;
        }

        final boolean somethingConnected = processQueue(readySessions.getToConnect(), session -> {
            if (session instanceof StreamSession streamSession) {
                LOGGER.trace("Connected {}", session);
                if (session.getState() != Session.States.NEW) {
                    return false;
                }
                receiver.receive(streamSession);
                try {
                    final SocketChannel channel = getChannel(streamSession);
                    channel.finishConnect();
                    streamSession.connect();
                    return true;
                } catch (final ConnectException exception) {
                    LOGGER.trace("Connection rejected for {}", session);
                    closeSession(session);
                    return true;
                } catch (final IOException exception) {
                    LOGGER.error("Error on socket.finishConnect()", exception);
                    closeSession(session);
                    return true;
                }
            }
            return false;
        });
        if (somethingConnected) {
            return;
        }

        processQueue(readySessions.getToRead(), session -> {
            if (session instanceof DatagramSession datagramSession) {
                LOGGER.trace("Datagram received");
                final DatagramChannel channel = getChannel(datagramSession);
                try {
                    final ByteBuffer datagram = receiver.receive(datagramSession);
                    assert datagram != null;
                    final SocketAddress address = channel.receive(datagram);
                    if (address == null) {
                        return false;
                    }
                    if (Config.useSynchronisedNAT && !address.equals(datagramSession.getDestination())) {
                        return false;
                    }
                    datagram.flip();
                    return true;
                } catch (final IOException exception) {
                    LOGGER.error("Trying to read datagram socket", exception);
                }
                LOGGER.trace("Datagram received");
            } else if (session instanceof StreamSession streamSession) {
                LOGGER.trace("Stream received");
                final ByteBuffer stream = receiver.receive(streamSession);
                try {
                    final SocketChannel channel = getChannel(streamSession);
                    assert stream != null;
                    assert false;
                    final int read = channel.read(stream);
                    LOGGER.trace("Read from real world: {}", read);
                    if (read == -1) {
                        closeSession(session);
                    }
                    return true;
                } catch (final IOException exception) {
                    LOGGER.error("Trying to read stream socket", exception);
                }
            }
            return false;
        });
    }

    @Override
    public void sendSession(final Session session, @Nullable final ByteBuffer data) {
        if (session instanceof final EchoSession echoSession) {
            if (data == null) {
                return; // session closed due expiration
            }
            final EchoResponse response = new EchoResponse(data, echoSession);
            final InetAddress address = session.getDestination().getAddress();
            executor.execute(() -> {
                try {
                    if (address.isReachable(null, echoSession.getTtl(), Config.defaultEchoRequestTimeoutMs)) {
                        echoResponse.set(response);
                    }
                } catch (IOException e) {
                    LOGGER.error("Failed to get echo response", e);
                }
            });
        } else if (session instanceof DatagramSession datagramSession) {
            try {
                switch (session.getState()) {
                    case NEW: {
                        final DatagramChannel channel =
                            socketManager.createDatagramChannel(datagramSession, readySessions);
                        datagramSession.setAttachment(channel);
                        LOGGER.trace("Open datagram socket {}", session.getDestination());
                        /* Fallthrough */
                    }
                    case ESTABLISHED: {
                        LOGGER.trace("Send datagram");
                        final DatagramChannel channel = getChannel(datagramSession);
                        assert data != null;
                        channel.send(data, session.getDestination());
                        break;
                    }
                    case EXPIRED: {
                        closeSession(session);
                        LOGGER.trace("Close datagram socket {}", session.getDestination());
                        break;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Datagram session failure", e);
                session.close();
            }
        } else if (session instanceof StreamSession streamSession) {
            try {
                switch (session.getState()) {
                    case NEW -> {
                        final SocketChannel channel = socketManager.createStreamChannel(streamSession, readySessions);
                        streamSession.setAttachment(channel);
                        channel.connect(streamSession.getDestination());
                        LOGGER.trace("Open stream socket {}", streamSession.getDestination());
                    }
                    case ESTABLISHED -> {
                        final SocketChannel channel = getChannel(streamSession);
                        assert data != null;
                        channel.write(data);
                    }
                    case FINISH, EXPIRED -> {
                        closeSession(session);
                        LOGGER.trace("Close stream socket {}", session.getDestination());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Stream session failure", e);
                session.close();
            }
        } else {
            session.close();
        }
    }

    private boolean processQueue(final Queue<Session> queue, final Function<Session, Boolean> action) {
        while (true) {
            final Session session = queue.poll();
            if (session == null) {
                return false;
            }
            if (session.isClosed()) {
                continue;
            }
            if (action.apply(session)) {
                return true;
            }
        }
    }

    private void closeSession(final Session session) {
        try {
            getChannel(session).close();
            if (!session.isClosed()) {
                session.close();
            }
        } catch (final IOException exception) {
            LOGGER.error("Error on closing channel", exception);
        }
    }

    private Object getExistingUserdata(final Session session) {
        final Object channel = session.getAttachment();
        assert channel != null;
        return channel;
    }

    private SocketChannel getChannel(final StreamSession session) {
        return (SocketChannel) getExistingUserdata(session);
    }

    private DatagramChannel getChannel(final DatagramSession session) {
        return (DatagramChannel) getExistingUserdata(session);
    }

    private SelectableChannel getChannel(final Session session) {
        return (SelectableChannel) getExistingUserdata(session);
    }

    ///////////////////////////////////////////////////////////////////

    private static final class EchoResponse {
        final byte[] payload;
        final EchoSession session;

        public EchoResponse(final ByteBuffer payload, final EchoSession session) {
            this.payload = new byte[payload.remaining()];
            payload.get(this.payload);
            this.session = session;
        }
    }
}

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

        final boolean somethingRead = processQueue(readySessions.getToRead(), session -> {
            if (session instanceof DatagramSession datagramSession) {
                LOGGER.info("Datagram received");
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
                LOGGER.info("Datagram received");
            } else if (session instanceof StreamSession streamSession) {
                LOGGER.info("Stream received");
                final SocketChannel channel = getChannel(streamSession);
                try {
                    final ByteBuffer stream = receiver.receive(streamSession);
                    assert stream != null;
                    final int read = channel.read(stream);
                    if (read != 0) {
                        // some data still remaining in socket, read it later
                        readySessions.getToRead().add(streamSession);
                    }
                    return true;
                } catch (final IOException exception) {
                    LOGGER.error("Trying to read stream socket", exception);
                }
            }
            return false;
        });
        if (somethingRead) {
            return;
        }

        processQueue(readySessions.getToConnect(), session -> {
            if (session instanceof StreamSession streamSession) {
                receiver.receive(streamSession);
                streamSession.connect();
                return true;
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
                        datagramSession.setUserdata(channel);
                        LOGGER.info("Open datagram socket {}", session.getDestination());
                        /* Fallthrough */
                    }
                    case ESTABLISHED: {
                        LOGGER.info("Send datagram");
                        final DatagramChannel channel = getChannel(datagramSession);
                        assert data != null;
                        channel.send(data, session.getDestination());
                        break;
                    }
                    case EXPIRED: {
                        closeSession(session);
                        LOGGER.info("Close datagram socket {}", session.getDestination());
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
                        session.setUserdata(channel);
                        channel.connect(session.getDestination());
                        LOGGER.info("Open stream socket {}", session.getDestination());
                    }
                    case ESTABLISHED -> {
                        final SocketChannel channel = getChannel(streamSession);
                        assert data != null;
                        channel.write(data);
                    }
                    case FINISH, EXPIRED -> {
                        closeSession(session);
                        LOGGER.info("Close stream socket {}", session.getDestination());
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
        } catch (final IOException exception) {
            LOGGER.error("Error on closing channel", exception);
        }
    }

    private Object getExistingUserdata(final Session session) {
        final Object channel = session.getUserdata();
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

package li.cil.oc2.common.inet;

import li.cil.oc2.api.inet.*;
import li.cil.oc2.common.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

public final class DefaultSessionLayer implements SessionLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final Executor executor = Executors
            .newSingleThreadExecutor(runnable -> new Thread(runnable, "internet/blocking-session"));

    ///////////////////////////////////////////////////////////////////

    private final AtomicReference<EchoResponse> echoResponse = new AtomicReference<>(null);

    ///////////////////////////////////////////////////////////////////

    private final Set<Session> readySessions = new HashSet<>();
    private final Set<SelectionKey> ownedKeys = new HashSet<>();
    private final SocketManager socketManager;

    public DefaultSessionLayer(final LayerParameters layerParameters) {
        InternetManager internetManager = layerParameters.getInternetManager();
        socketManager = SocketManager.attach(internetManager);
    }

    @Override
    public void onStop() {
        for (var key : ownedKeys) {
            try {
                key.channel().close();
            } catch (final IOException exception) {
                LOGGER.error("Exception on stopping channel", exception);
            }
        }
        ownedKeys.clear();
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

        for (final Session session : readySessions) {
            if (session instanceof DatagramSession datagramSession) {
                final SelectionKey selectionKey = (SelectionKey) datagramSession.getUserdata();
                assert selectionKey != null;
                if (selectionKey.isReadable()) {
                    LOGGER.info("Datagram received");
                    final DatagramChannel channel = (DatagramChannel) selectionKey.channel();
                    try {
                        final ByteBuffer datagram = receiver.receive(datagramSession);
                        assert datagram != null;
                        final SocketAddress address = channel.receive(datagram);
                        if (address == null) {
                            continue;
                        }
                        if (Config.useSynchronisedNAT && !address.equals(datagramSession.getDestination())) {
                            continue;
                        }
                        datagram.flip();
                        return;
                    } catch (IOException e) {
                        LOGGER.error("Trying to read datagram socket", e);
                    }
                }
            }
        }
        readySessions.clear();
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
                        final DatagramChannel channel = DatagramChannel.open();
                        channel.configureBlocking(false);
                        final SelectionKey key = socketManager.createDatagramChannel(datagramSession, readySessions);
                        datagramSession.setUserdata(key);
                        ownedKeys.add(key);
                        LOGGER.info("Open datagram socket {}", session.getDestination());
                        /* Fallthrough */
                    }
                    case ESTABLISHED: {
                        final SelectionKey key = (SelectionKey) session.getUserdata();
                        assert key != null;
                        if (key.isWritable()) {
                            final DatagramChannel channel = (DatagramChannel) key.channel();
                            assert data != null;
                            channel.send(data, session.getDestination());
                        }
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
        }
        /* else if (session instanceof StreamSession) {
            try {
                switch (session.getState()) {
                    case NEW -> {
                        final SocketChannel channel = SocketChannel.open();
                        channel.configureBlocking(false);
                        final SelectionKey key =
                                register(channel, session, SelectionKey.OP_READ | SelectionKey.OP_WRITE | SelectionKey.OP_CONNECT);
                        session.setUserdata(key);
                        streamKeys.add(key);
                        channel.connect(session.getDestination());
                        LOGGER.info("Open stream socket {}", session.getDestination());
                    }
                    case ESTABLISHED -> {
                        final SelectionKey key = (SelectionKey) session.getUserdata();
                        assert key != null;
                        if (key.isWritable()) {
                            final SocketChannel channel = (SocketChannel) key.channel();
                            assert data != null;
                            channel.write(data);
                        }
                    }
                    case FINISH, EXPIRED -> {
                        final SelectionKey key = (SelectionKey) session.getUserdata();
                        assert key != null;
                        key.channel().close();
                        streamKeys.remove(key);
                        LOGGER.info("Close stream socket {}", session.getDestination());
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Stream session failure", e);
                session.close();
            }
        }
        */
        else {
            session.close();
        }
    }

    private void closeSession(final Session session) {
        try {
            readySessions.remove(session);
            final SelectionKey selectionKey = (SelectionKey) session.getUserdata();
            assert selectionKey != null;
            selectionKey.channel().close();
            ownedKeys.remove(selectionKey);
        } catch (final IOException exception) {
            LOGGER.error("Error on closing channel", exception);
        }
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

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.socket;

import li.cil.oc2.common.inet.l2.LinkLocalLayer;
import li.cil.oc2.common.inet.l3.NetworkLayer;
import li.cil.oc2.common.inet.l4.AbstractSession;
import li.cil.oc2.common.inet.l4.DatagramSession;
import li.cil.oc2.common.inet.l4.EchoSession;
import li.cil.oc2.common.inet.l4.SessionLayer;
import li.cil.oc2.common.inet.l4.StreamSession;
import li.cil.oc2.common.inet.l4.TransportLayer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.NoRouteToHostException;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public final class SocketSessionLayer implements SessionLayer {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int MAX_ECHO_PAYLOAD =
        LinkLocalLayer.DEFAULT_MTU - NetworkLayer.IPv4_HEADER_SIZE - TransportLayer.ICMP_HEADER_SIZE;

    // --------------------------------------------------------------------- //

    private final SocketManager socketManager;
    private final String originDescription;
    private final SocketManager.ReadySessions ready = new SocketManager.ReadySessions();
    private final Executor echoExecutor;
    private final int echoTimeoutMs;
    private final Set<AbstractSession> pendingWrites = new LinkedHashSet<>();
    private final Set<AbstractSession> openSessions = new LinkedHashSet<>();
    private final AtomicReference<EchoResponse> echoResponse = new AtomicReference<>();

    // --------------------------------------------------------------------- //

    public SocketSessionLayer(
        final String originDescription,
        final SocketManager socketManager,
        final Executor echoExecutor,
        final int echoTimeoutMs
    ) {
        this.originDescription = originDescription;
        this.socketManager = socketManager;
        this.echoExecutor = echoExecutor;
        this.echoTimeoutMs = echoTimeoutMs;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void onTick() {
        final Iterator<AbstractSession> iterator = pendingWrites.iterator();
        while (iterator.hasNext()) {
            final AbstractSession session = iterator.next();
            if (session.isClosed() || !(session instanceof final StreamSession stream)) {
                iterator.remove();
                continue;
            }
            if (flushStream(stream)) {
                iterator.remove();
            }
        }
    }

    @Override
    public void onStop() {
        for (final AbstractSession session : List.copyOf(openSessions)) {
            closeSession(session);
        }
        openSessions.clear();
        pendingWrites.clear();
        ready.clear();
        echoResponse.set(null);
    }

    @Override
    public void receiveSession(final Receiver receiver) {
        final EchoResponse pending = echoResponse.getAndSet(null);
        if (pending != null && !pending.session().isClosed()) {
            pending.session().setSequenceNumber((short) pending.sequenceNumber());
            final ByteBuffer buffer = receiver.receive(pending.session());
            if (buffer != null) {
                buffer.put(pending.payload(), 0, Math.min(pending.payload().length, buffer.remaining()));
            } else {
                receiver.cancel();
            }
            return;
        }

        if (processConnects()) {
            return;
        }
        processReads(receiver);
    }

    @Override
    public void sendSession(final AbstractSession session, @Nullable final ByteBuffer data) {
        if (session instanceof final EchoSession echo) {
            sendEcho(echo, data);
        } else if (session instanceof final DatagramSession datagram) {
            sendDatagram(datagram, data);
        } else if (session instanceof final StreamSession stream) {
            sendStream(stream);
        } else {
            session.close();
        }
    }

    // --------------------------------------------------------------------- //

    private boolean processConnects() {
        final Iterator<AbstractSession> iterator = ready.toConnect().iterator();
        while (iterator.hasNext()) {
            final AbstractSession session = iterator.next();
            iterator.remove();
            if (session.isClosed() || !(session instanceof final StreamSession stream)) {
                continue;
            }
            final SocketChannel channel = (SocketChannel) session.attachment();
            if (channel == null) {
                continue;
            }
            try {
                if (channel.finishConnect()) {
                    stream.connect();
                }
            } catch (final ConnectException | NoRouteToHostException e) {
                closeSession(session);
                session.close();
            } catch (final IOException e) {
                closeSession(session);
                session.close();
            }
            return true;
        }
        return false;
    }

    private void processReads(final Receiver receiver) {
        final Iterator<AbstractSession> iterator = ready.toRead().iterator();
        while (iterator.hasNext()) {
            final AbstractSession session = iterator.next();
            if (session.isClosed()) {
                iterator.remove();
                continue;
            }
            if (session instanceof final StreamSession stream) {
                iterator.remove();
                if (readStream(stream, receiver)) {
                    return;
                }
            } else if (session instanceof final DatagramSession datagram) {
                iterator.remove();
                if (readDatagram(datagram, receiver)) {
                    return;
                }
            } else {
                iterator.remove();
            }
        }
    }

    private boolean readStream(final StreamSession stream, final Receiver receiver) {
        final SocketChannel channel = (SocketChannel) stream.attachment();
        if (channel == null) {
            return false;
        }
        final ByteBuffer buffer = receiver.receive(stream);
        if (buffer == null) {
            return false;
        }
        if (!buffer.hasRemaining()) {
            // Nowhere to put the data; the selector will offer it again.
            receiver.cancel();
            return false;
        }
        try {
            final int read = channel.read(buffer);
            if (read < 0) {
                stream.finishReceiving();
                socketManager.stopReading(channel);
                receiver.cancel();
                return false;
            }
            if (read == 0) {
                receiver.cancel();
                return false;
            }
            return true;
        } catch (final IOException e) {
            receiver.cancel();
            closeSession(stream);
            stream.close();
            return false;
        }
    }

    private boolean readDatagram(final DatagramSession session, final Receiver receiver) {
        final DatagramChannel channel = (DatagramChannel) session.attachment();
        if (channel == null) {
            return false;
        }
        final ByteBuffer buffer = receiver.receive(session);
        if (buffer == null) {
            receiver.cancel();
            return false;
        }
        try {
            if (channel.receive(buffer) == null) {
                receiver.cancel();
                return false;
            }
            return true;
        } catch (final SocketException e) {
            receiver.cancel();
            closeSession(session);
            session.close();
            return false;
        } catch (final IOException e) {
            receiver.cancel();
            closeSession(session);
            session.close();
            return false;
        }
    }

    private void sendEcho(final EchoSession session, @Nullable final ByteBuffer data) {
        switch (session.getState()) {
            case FINISH, REJECT, EXPIRED -> {
                return;
            }
            default -> {
            }
        }
        if (data == null) {
            return;
        }

        final int length = Math.min(data.remaining(), MAX_ECHO_PAYLOAD);
        final byte[] payload = new byte[length];
        data.duplicate().get(payload);
        final InetAddress address = session.getDestination().getAddress();
        final int timeToLive = session.getTimeToLive();
        final int sequenceNumber = session.getSequenceNumber();

        echoExecutor.execute(() -> {
            try {
                if (address.isReachable(null, timeToLive, echoTimeoutMs)) {
                    echoResponse.set(new EchoResponse(session, sequenceNumber, payload));
                }
            } catch (final IOException e) {
                // Unreachable is an answer; the guest sees the probe time out.
            }
        });
    }

    private void sendDatagram(final DatagramSession session, @Nullable final ByteBuffer data) {
        try {
            switch (session.getState()) {
                case NEW -> {
                    final DatagramChannel channel = socketManager.openDatagramChannel(session, ready);
                    session.attach(channel);
                    openSessions.add(session);
                    channel.connect(session.getDestination());
                    LOGGER.info("Internet gateway at {} opening datagram socket to {}.",
                        originDescription, session.getDestination());
                    writeDatagram(channel, data);
                }
                case ESTABLISHED -> {
                    final DatagramChannel channel = (DatagramChannel) session.attachment();
                    if (channel != null) {
                        writeDatagram(channel, data);
                    }
                }
                case FINISH, REJECT, EXPIRED -> closeSession(session);
            }
        } catch (final IOException e) {
            closeSession(session);
            session.close();
        }
    }

    private void writeDatagram(final DatagramChannel channel, @Nullable final ByteBuffer data) throws IOException {
        if (data == null || !data.hasRemaining()) {
            return;
        }
        channel.write(data);
    }

    private void sendStream(final StreamSession stream) {
        try {
            switch (stream.getState()) {
                case NEW -> {
                    if (stream.attachment() == null) {
                        final SocketChannel channel = socketManager.openSocketChannel(stream, ready);
                        stream.attach(channel);
                        openSessions.add(stream);
                        LOGGER.info("Internet gateway at {} connecting to {}.",
                            originDescription, stream.getDestination());
                        if (channel.connect(stream.getDestination())) {
                            stream.connect();
                        }
                    }
                }
                case ESTABLISHED -> {
                    if (!flushStream(stream)) {
                        pendingWrites.add(stream);
                    }
                }
                case FINISH -> {
                    flushStream(stream);
                    closeSession(stream);
                }
                case REJECT, EXPIRED -> closeSession(stream);
            }
        } catch (final IOException e) {
            closeSession(stream);
            stream.close();
        }
    }

    private boolean flushStream(final StreamSession stream) {
        final SocketChannel channel = (SocketChannel) stream.attachment();
        if (channel == null || !channel.isOpen()) {
            return true;
        }
        try {
            final ByteBuffer data = stream.getSendBuffer();
            if (data.hasRemaining() && channel.isConnected()) {
                channel.write(data);
            }
            if (data.hasRemaining()) {
                return false;
            }
            if (stream.isGuestHalfClosed() && channel.isConnected()) {
                channel.shutdownOutput();
            }
            return true;
        } catch (final IOException e) {
            closeSession(stream);
            stream.close();
            return true;
        }
    }

    private void closeSession(final AbstractSession session) {
        openSessions.remove(session);
        pendingWrites.remove(session);
        ready.toRead().remove(session);
        ready.toConnect().remove(session);

        final Object attachment = session.attachment();
        session.attach(null);
        if (attachment instanceof final Channel channel) {
            try {
                channel.close();
            } catch (final IOException e) {
                // Nothing useful to do about a channel that will not close.
            }
        }
    }

    // --------------------------------------------------------------------- //

    private record EchoResponse(EchoSession session, int sequenceNumber, byte[] payload) {
    }
}

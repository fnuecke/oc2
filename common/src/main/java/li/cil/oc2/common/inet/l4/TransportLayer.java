/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import li.cil.oc2.common.inet.util.InternetUtils;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.function.IntSupplier;

public final class TransportLayer {
    public static final byte PROTOCOL_NONE = 0;
    public static final byte PROTOCOL_ICMP = 1;
    public static final byte PROTOCOL_TCP = 6;
    public static final byte PROTOCOL_UDP = 17;
    public static final int ICMP_HEADER_SIZE = 8;

    private static final byte ICMP_TYPE_ECHO_REPLY = 0;
    private static final byte ICMP_TYPE_ECHO_REQUEST = 8;
    private static final short PORT_ECHO = 7;
    private static final int UDP_HEADER_SIZE = 8;
    private static final int UDP_CHECKSUM_OFFSET = 6;
    private static final int REPLY_HEADER_SIZE = 8;
    private static final int MAX_PENDING_RESETS = 8;
    private static final long EXPIRY_SWEEP_INTERVAL_NANOS = 50_000_000L;

    // --------------------------------------------------------------------- //

    private final SessionLayer sessionLayer;
    private final PortFilter portFilter;
    private final SessionLimits limits;
    private final TokenBucket sessionRate;
    private final IntSupplier gatewayCount;
    private final StreamSession.TcpConfig tcpConfig;
    private final long sessionTimeoutNanos;
    private final Map<SessionKey, AbstractSession> sessions = new LinkedHashMap<>();
    private final Deque<PendingReset> pendingResets = new ArrayDeque<>();
    private final SessionReceiver receiver = new SessionReceiver();
    private final TcpHeader header = new TcpHeader();
    private long lastExpirySweep;

    // --------------------------------------------------------------------- //

    public TransportLayer(
        final SessionLayer sessionLayer,
        final PortFilter portFilter,
        final SessionLimits limits,
        final TokenBucket sessionRate,
        final IntSupplier gatewayCount,
        final StreamSession.TcpConfig tcpConfig,
        final long sessionTimeoutNanos
    ) {
        this.sessionLayer = sessionLayer;
        this.portFilter = portFilter;
        this.limits = limits;
        this.sessionRate = sessionRate;
        this.gatewayCount = gatewayCount;
        this.tcpConfig = tcpConfig;
        this.sessionTimeoutNanos = sessionTimeoutNanos;
    }

    // --------------------------------------------------------------------- //

    public void onTick() {
        sessionLayer.onTick();
    }

    public void onStop() {
        for (final AbstractSession session : List.copyOf(sessions.values())) {
            session.expire();
            sessionLayer.sendSession(session, null);
            limits.release();
        }
        sessions.clear();
        pendingResets.clear();
        sessionLayer.onStop();
    }

    public void sendTransportMessage(final byte protocol, final TransportMessage message) {
        expireIdleSessions();

        final int sourceIpAddress = message.getSourceIpv4Address();
        final int destinationIpAddress = message.getDestinationIpv4Address();
        final ByteBuffer data = message.getData();

        switch (protocol) {
            case PROTOCOL_ICMP -> sendIcmp(data, sourceIpAddress, destinationIpAddress);
            case PROTOCOL_UDP -> sendUdp(data, sourceIpAddress, destinationIpAddress);
            case PROTOCOL_TCP -> sendTcp(data, sourceIpAddress, destinationIpAddress);
            default -> {
            }
        }
    }

    public byte receiveTransportMessage(final TransportMessage message) {
        expireIdleSessions();

        if (!pendingResets.isEmpty()) {
            return writeReset(message, pendingResets.poll());
        }

        final byte fromSessionLayer = pollSessionLayer(message);
        if (fromSessionLayer != PROTOCOL_NONE) {
            return fromSessionLayer;
        }

        return pollStreams(message);
    }

    // --------------------------------------------------------------------- //

    private void sendIcmp(
        final ByteBuffer data,
        final int sourceIpAddress,
        final int destinationIpAddress
    ) {
        if (data.remaining() < ICMP_HEADER_SIZE) {
            return;
        }
        final byte type = data.get();
        final byte code = data.get();
        data.getShort(); // Checksum; the link to the guest cannot corrupt anything.

        if (type != ICMP_TYPE_ECHO_REQUEST || code != 0) {
            return;
        }

        final short identity = data.getShort();
        final short sequence = data.getShort();

        final SessionKey.Echo key = new SessionKey.Echo(sourceIpAddress, destinationIpAddress, identity);
        final EchoSession session = getOrCreateSession(key, it -> new EchoSession(it, PORT_ECHO));
        if (session == null) {
            return;
        }

        session.touch();
        session.setSequenceNumber(sequence);
        sessionLayer.sendSession(session, data);
        afterSend(session);
    }

    private void sendUdp(final ByteBuffer data, final int sourceIpAddress, final int destinationIpAddress) {
        if (data.remaining() < UDP_HEADER_SIZE) {
            return;
        }
        final short sourcePort = data.getShort();
        final short destinationPort = data.getShort();
        final int datagramLength = Short.toUnsignedInt(data.getShort());
        data.getShort(); // Checksum.

        if (datagramLength < UDP_HEADER_SIZE || datagramLength - UDP_HEADER_SIZE > data.remaining()) {
            return;
        }
        data.limit(data.position() + datagramLength - UDP_HEADER_SIZE);

        if (!portFilter.isAllowed(destinationPort)) {
            return;
        }

        final SessionKey.Datagram key = new SessionKey.Datagram(sourceIpAddress, sourcePort, destinationIpAddress, destinationPort);
        final DatagramSession session = getOrCreateSession(key, DatagramSession::new);
        if (session == null) {
            return;
        }

        session.touch();
        sessionLayer.sendSession(session, data);
        afterSend(session);
    }

    private void sendTcp(final ByteBuffer data, final int sourceIpAddress, final int destinationIpAddress) {
        if (data.remaining() < TcpHeader.MIN_HEADER_SIZE) {
            return;
        }
        final long now = System.nanoTime();
        final short sourcePort = data.getShort();
        final short destinationPort = data.getShort();

        if (!header.read(data)) {
            return;
        }

        if (!portFilter.isAllowed(destinationPort)) {
            queueReset(new SessionKey.Stream(sourceIpAddress, sourcePort, destinationIpAddress, destinationPort), header);
            return;
        }

        final SessionKey.Stream key = new SessionKey.Stream(sourceIpAddress, sourcePort, destinationIpAddress, destinationPort);
        StreamSession session = (StreamSession) sessions.get(key);

        if (session == null) {
            if (header.rst) {
                return;
            }
            if (!header.syn) {
                queueReset(key, header);
                return;
            }
            session = getOrCreateSession(key, it -> new StreamSession(it, tcpConfig));
            if (session == null) {
                queueReset(key, header);
                return;
            }
            session.onSegment(header, data, now);
            if (session.isFinished()) {
                retireStream(session, now);
            } else {
                sessionLayer.sendSession(session, null);
            }
            return;
        }

        session.onSegment(header, data, now);

        if (session.isFinished()) {
            retireStream(session, now);
        } else if (session.getState() == SessionState.ESTABLISHED) {
            sessionLayer.sendSession(session, session.getSendBuffer());
        }
    }

    private void queueReset(final SessionKey.Stream key, final TcpHeader header) {
        if (pendingResets.size() >= MAX_PENDING_RESETS) {
            return;
        }
        pendingResets.add(PendingReset.forSegment(key, header));
    }

    private void retireStream(final StreamSession stream, final long now) {
        sessionLayer.sendSession(stream, null);
        if (!stream.wantsToSend(now)) {
            closeSession(stream);
        }
    }

    private void afterSend(final AbstractSession session) {
        switch (session.getState()) {
            case NEW -> setSessionState(session, SessionState.ESTABLISHED);
            case FINISH, REJECT, EXPIRED -> closeSession(session);
            case ESTABLISHED -> {
            }
        }
    }

    private static void setSessionState(final AbstractSession session, final SessionState state) {
        if (session instanceof final DatagramSession datagram) {
            datagram.setState(state);
        } else if (session instanceof final EchoSession echo) {
            echo.setState(state);
        }
    }

    private byte pollSessionLayer(final TransportMessage message) {
        receiver.prepare(message.getData());
        sessionLayer.receiveSession(receiver);

        final AbstractSession session = receiver.session;
        if (session == null) {
            return PROTOCOL_NONE;
        }
        session.touch();
        moveToBack(session);

        if (session instanceof final EchoSession echo) {
            if (echo.getState() != SessionState.ESTABLISHED) {
                sessionLayer.sendSession(echo, null);
                closeSession(echo);
                return PROTOCOL_NONE;
            }
            return writeEchoReply(message, echo);
        }
        if (session instanceof final DatagramSession datagram) {
            if (datagram.getState() != SessionState.ESTABLISHED) {
                sessionLayer.sendSession(datagram, null);
                closeSession(datagram);
                return PROTOCOL_NONE;
            }
            return writeDatagram(message, datagram);
        }
        return PROTOCOL_NONE;
    }

    private byte pollStreams(final TransportMessage message) {
        final long now = System.nanoTime();
        StreamSession ready = null;
        for (final AbstractSession session : sessions.values()) {
            if (session instanceof final StreamSession stream && stream.wantsToSend(now)) {
                ready = stream;
                break;
            }
        }
        if (ready == null) {
            return PROTOCOL_NONE;
        }

        final SessionKey.Stream key = ready.getKey();
        final ByteBuffer data = message.getData();
        final int start = data.position();
        data.putShort(key.destinationPort());
        data.putShort(key.sourcePort());

        if (!ready.writeSegment(header, data, now)) {
            data.position(start);
            moveToBack(ready);
            return PROTOCOL_NONE;
        }

        data.limit(data.position());
        data.position(start);
        writeTransportChecksum(data, key.destinationIpAddress(), key.sourceIpAddress(), PROTOCOL_TCP, TcpHeader.CHECKSUM_OFFSET);
        message.updateIpv4(key.destinationIpAddress(), key.sourceIpAddress());

        moveToBack(ready);
        if (ready.isFinished()) {
            sessionLayer.sendSession(ready, null);
            closeSession(ready);
        }
        return PROTOCOL_TCP;
    }

    private byte writeEchoReply(final TransportMessage message, final EchoSession echo) {
        final SessionKey.Echo key = echo.getKey();
        final ByteBuffer data = receiver.getBuffer();
        final int start = data.position();
        data.put(start, ICMP_TYPE_ECHO_REPLY);
        data.put(start + 1, (byte) 0);
        data.putShort(start + 2, (short) 0); // Checksum, computed over the finished message below.
        data.putShort(start + 4, key.identity());
        data.putShort(start + 6, (short) echo.getSequenceNumber());

        final short checksum = InternetUtils.rfc1071Checksum(data);
        data.putShort(start + 2, checksum);
        data.position(start);

        message.updateIpv4(key.destinationIpAddress(), key.sourceIpAddress());
        return PROTOCOL_ICMP;
    }

    private byte writeDatagram(final TransportMessage message, final DatagramSession datagram) {
        final SessionKey.Datagram key = datagram.getKey();
        final ByteBuffer data = receiver.getBuffer();
        final int start = data.position();
        data.putShort(start, key.destinationPort());
        data.putShort(start + 2, key.sourcePort());
        data.putShort(start + 4, (short) data.remaining());
        data.putShort(start + 6, (short) 0); // Checksum, computed over the finished message below.
        writeTransportChecksum(data, key.destinationIpAddress(), key.sourceIpAddress(), PROTOCOL_UDP, UDP_CHECKSUM_OFFSET);
        if (data.getShort(start + UDP_CHECKSUM_OFFSET) == 0) {
            data.putShort(start + UDP_CHECKSUM_OFFSET, (short) 0xFFFF);
        }
        message.updateIpv4(key.destinationIpAddress(), key.sourceIpAddress());
        return PROTOCOL_UDP;
    }

    private byte writeReset(final TransportMessage message, final PendingReset reset) {
        final ByteBuffer data = message.getData();
        final int start = data.position();
        data.putShort(reset.key().destinationPort());
        data.putShort(reset.key().sourcePort());

        header.clear();
        header.rst = true;
        header.ack = reset.withAck();
        header.sequenceNumber = reset.sequenceNumber();
        header.acknowledgmentNumber = reset.acknowledgmentNumber();
        header.write(data);

        data.limit(data.position());
        data.position(start);
        writeTransportChecksum(data,
            reset.key().destinationIpAddress(), reset.key().sourceIpAddress(), PROTOCOL_TCP, TcpHeader.CHECKSUM_OFFSET);
        message.updateIpv4(reset.key().destinationIpAddress(), reset.key().sourceIpAddress());
        return PROTOCOL_TCP;
    }

    private static void writeTransportChecksum(
        final ByteBuffer data,
        final int sourceIpAddress,
        final int destinationIpAddress,
        final byte protocol,
        final int checksumOffset
    ) {
        final int start = data.position();
        final short checksum = InternetUtils.transportRfc1071Checksum(data, sourceIpAddress, destinationIpAddress, protocol);
        data.putShort(start + checksumOffset, checksum);
        data.position(start);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    private <K extends SessionKey, S extends AbstractSession> S getOrCreateSession(
        final K key,
        final java.util.function.Function<K, S> factory
    ) {
        final AbstractSession existing = sessions.get(key);
        if (existing != null) {
            return (S) existing;
        }
        if (!limits.tryAcquire(sessions.size(), gatewayCount.getAsInt())) {
            return null;
        }
        if (!sessionRate.tryAcquire(System.nanoTime())) {
            limits.release();
            return null;
        }
        final S session = factory.apply(key);
        sessions.put(key, session);
        return session;
    }

    private void closeSession(final AbstractSession session) {
        if (sessions.remove(session.getKey()) != null) {
            limits.release();
        }
    }

    private void moveToBack(final AbstractSession session) {
        if (sessions.remove(session.getKey()) != null) {
            sessions.put(session.getKey(), session);
        }
    }

    private void expireIdleSessions() {
        if (sessions.isEmpty()) {
            return;
        }
        final long now = System.nanoTime();
        if (now - lastExpirySweep < EXPIRY_SWEEP_INTERVAL_NANOS) {
            return;
        }
        lastExpirySweep = now;
        List<AbstractSession> expired = null;
        for (final AbstractSession session : sessions.values()) {
            if (now - session.getLastUpdateTime() >= sessionTimeoutNanos) {
                if (expired == null) {
                    expired = new ArrayList<>();
                }
                expired.add(session);
            }
        }
        if (expired == null) {
            return;
        }
        for (final AbstractSession session : expired) {
            session.expire();
            if (session instanceof final StreamSession stream) {
                retireStream(stream, now);
            } else {
                sessionLayer.sendSession(session, null);
                closeSession(session);
            }
        }
    }

    // --------------------------------------------------------------------- //

    private record PendingReset(
        SessionKey.Stream key,
        int sequenceNumber,
        int acknowledgmentNumber,
        boolean withAck
    ) {
        static PendingReset forSegment(final SessionKey.Stream key, final TcpHeader header) {
            if (header.ack) {
                return new PendingReset(key, header.acknowledgmentNumber, 0, false);
            }
            return new PendingReset(key, 0, header.sequenceNumber + 1, true);
        }
    }

    private static final class SessionReceiver implements SessionLayer.Receiver {
        @Nullable
        private AbstractSession session;
        @Nullable
        private ByteBuffer buffer;
        private int start;
        private int limit;

        void prepare(final ByteBuffer buffer) {
            this.session = null;
            this.buffer = buffer;
            this.start = buffer.position();
            this.limit = buffer.limit();
        }

        ByteBuffer getBuffer() {
            final ByteBuffer buffer = this.buffer;
            if (buffer == null) {
                throw new IllegalStateException("No buffer prepared.");
            }
            buffer.limit(buffer.position());
            buffer.position(start);
            return buffer;
        }

        @Override
        public void cancel() {
            session = null;
            final ByteBuffer buffer = this.buffer;
            if (buffer != null) {
                buffer.position(start);
                buffer.limit(limit);
            }
        }

        @Nullable
        @Override
        public ByteBuffer receive(final AbstractSession session) {
            final ByteBuffer buffer = this.buffer;
            if (buffer == null) {
                throw new IllegalStateException("No buffer prepared.");
            }
            buffer.position(start);
            buffer.limit(limit);

            if (session instanceof final StreamSession stream) {
                this.session = stream;
                return stream.getReceiveBuffer();
            }

            if (session.getState() != SessionState.ESTABLISHED) {
                return null;
            }

            this.session = session;
            buffer.position(start + REPLY_HEADER_SIZE);
            return buffer;
        }
    }
}

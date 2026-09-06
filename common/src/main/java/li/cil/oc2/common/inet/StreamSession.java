/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.ByteBuffer;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public final class StreamSession extends AbstractSession {
    public record TcpConfig(int bufferSize, int initialRetransmissionTimeoutMs, int maxRetransmissionTimeoutMs) {
        public static final TcpConfig DEFAULT = new TcpConfig(32 * 1024, 250, 8000);
    }

    // --------------------------------------------------------------------- //

    private static final Logger LOGGER = LogManager.getLogger();

    public static final int DEFAULT_MAX_SEGMENT_SIZE =
        LinkLocalLayer.DEFAULT_MTU - NetworkLayer.IPv4_HEADER_SIZE - TcpHeader.MIN_HEADER_SIZE;
    private static final int MIN_PEER_MAX_SEGMENT_SIZE = 88;
    private static final int MAX_RETRANSMISSIONS = 8;
    private static final long MAX_PERSIST_NANOS = TimeUnit.MINUTES.toNanos(2);

    // --------------------------------------------------------------------- //

    private final SessionKey.Stream key;
    private final TcpConfig config;
    private final ByteBuffer toGuest;
    private final ByteBuffer toRemote;
    private TcpState state = TcpState.SYN_RECEIVED;

    // Send side: internet -> virtual machine.
    private final int initialSendSequence;

    private int sendUnacknowledged;
    private int sendNext;
    private int sendWindow;
    private int sendWindowUpdateSequence;
    private int sendWindowUpdateAcknowledgment;
    private int dataBase;
    private boolean synAcked;
    private boolean finSent;
    private int finSequence;
    private boolean finAcked;

    // Receive side: virtual machine -> internet.
    private int receiveNext;

    private int peerMaxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;
    private boolean remoteEof;
    private boolean guestFin;
    private boolean ackDue;
    private boolean resetDue;
    private boolean socketConnected;
    private boolean synReceived;

    // Retransmission timing, per RFC 6298.
    private long lastTransmitTime;

    private long retransmissionTimeoutNanos;
    private long smoothedRoundTripTimeNanos;
    private long roundTripTimeVarianceNanos;
    private int retransmissions;
    private int roundTripProbeSequence;
    private long roundTripProbeTime;
    private boolean hasRoundTripProbe;
    private int lastAdvertisedWindow;
    private boolean probeDue;
    private long persistSince;
    private int noRoundTripSampleBefore;
    private boolean hasRetransmitted;
    private long now; // stable per update

    // --------------------------------------------------------------------- //

    public StreamSession(final SessionKey.Stream key, final TcpConfig config) {
        super(key.destinationIpAddress(), key.destinationPort());
        this.key = key;
        this.config = config;
        this.toGuest = ByteBuffer.allocate(config.bufferSize());
        this.toRemote = ByteBuffer.allocate(config.bufferSize());
        this.toRemote.limit(0);
        this.initialSendSequence = ThreadLocalRandom.current().nextInt();
        this.sendUnacknowledged = initialSendSequence;
        this.sendNext = initialSendSequence;
        this.dataBase = initialSendSequence + 1;
        this.retransmissionTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(config.initialRetransmissionTimeoutMs());
    }

    // --------------------------------------------------------------------- //

    @Override
    public SessionKey.Stream getKey() {
        return key;
    }

    @Override
    public SessionState getState() {
        return state.toSessionState();
    }

    // Session layer interface
    public ByteBuffer getSendBuffer() {
        return toRemote;
    }

    public ByteBuffer getReceiveBuffer() {
        return toGuest;
    }

    public void connect() {
        if (state != TcpState.SYN_RECEIVED) {
            return;
        }
        socketConnected = true;
        lastTransmitTime = now - retransmissionTimeoutNanos;
    }

    public void finishReceiving() {
        remoteEof = true;
    }

    @Override
    public void close() {
        switch (state) {
            case SYN_RECEIVED -> {
                resetDue = true;
                state = TcpState.CLOSED;
            }
            case ESTABLISHED, CLOSE_WAIT -> remoteEof = true;
            default -> {
            }
        }
    }

    @Override
    public void expire() {
        if (state != TcpState.CLOSED) {
            resetDue = state == TcpState.ESTABLISHED || state == TcpState.CLOSE_WAIT;
            state = TcpState.EXPIRED;
        }
    }

    public boolean isGuestHalfClosed() {
        return guestFin && !toRemote.hasRemaining();
    }

    public boolean isFinished() {
        return state == TcpState.CLOSED || state == TcpState.EXPIRED;
    }

    public void onSegment(final TcpHeader header, final ByteBuffer payload, final long now) {
        this.now = now;
        touch();

        if (header.rst) {
            state = TcpState.CLOSED;
            return;
        }

        if (state == TcpState.SYN_RECEIVED) {
            onSegmentInSynReceived(header);
            return;
        }
        if (state == TcpState.CLOSED || state == TcpState.EXPIRED) {
            return;
        }

        if (header.syn) {
            // A SYN on a live connection means the peer lost its state. Tear ours down too.
            resetDue = true;
            state = TcpState.CLOSED;
            return;
        }

        if (!header.ack) {
            return;
        }

        if (!processAcknowledgment(header)) {
            return;
        }

        updateSendWindow(header);

        final int segmentEnd = header.sequenceNumber + payload.remaining();
        processPayload(header, payload);
        processFin(header, segmentEnd);
        checkCloseHandshake();
    }

    public boolean wantsToSend(final long now) {
        this.now = now;

        if (synAcked && lastAdvertisedWindow == 0 && computeWindow() > 0) {
            ackDue = true;
        }
        if (state == TcpState.EXPIRED && !resetDue) {
            return false;
        }
        if (resetDue || ackDue) {
            return true;
        }
        if (state == TcpState.SYN_RECEIVED) {
            return socketConnected && isSynAckDue(now);
        }
        if (state == TcpState.CLOSED) {
            return false;
        }
        if (isRetransmitDue(now)) {
            return true;
        }
        if (sendableDataLength() > 0) {
            return true;
        }
        return isFinDue();
    }

    public boolean writeSegment(final TcpHeader header, final ByteBuffer segment, final long now) {
        this.now = now;
        header.clear();

        if (resetDue) {
            resetDue = false;
            state = state == TcpState.EXPIRED ? TcpState.EXPIRED : TcpState.CLOSED;
            header.rst = true;
            header.ack = true;
            header.sequenceNumber = sendNext;
            header.acknowledgmentNumber = receiveNext;
            header.window = 0;
            header.write(segment);
            return true;
        }

        if (state == TcpState.SYN_RECEIVED) {
            if (!socketConnected || !isSynAckDue(now)) {
                return false;
            }
            return writeSynAck(header, segment, now);
        }

        if (isRetransmitDue(now)) {
            final boolean probing = sendWindow == 0 && unsentLength() > 0;
            if (probing && now - persistSince >= MAX_PERSIST_NANOS) {
                resetDue = true;
                state = TcpState.CLOSED;
                return writeSegment(header, segment, now);
            }
            if (!probing && ++retransmissions > MAX_RETRANSMISSIONS) {
                resetDue = true;
                state = TcpState.CLOSED;
                return writeSegment(header, segment, now);
            }
            // Go back to the oldest unacknowledged byte and resend from there. A round trip
            // measured across a retransmission is ambiguous, so block sampling until something
            // past everything sent so far is acknowledged (Karn's algorithm).
            noRoundTripSampleBefore = sendNext;
            hasRetransmitted = true;
            sendNext = sendUnacknowledged;
            finSent = finSent && TcpSequence.lt(finSequence, sendUnacknowledged);
            backOffRetransmitTimer();
            hasRoundTripProbe = false;
            probeDue = probing;
        }

        final int room = segment.remaining() - TcpHeader.SIZE_WITHOUT_PORTS;
        final int length = Math.min(sendableDataLength(), room);
        if (length > 0) {
            return writeData(header, segment, length, now);
        }

        if (isFinDue()) {
            return writeFin(header, segment, now);
        }

        if (ackDue) {
            ackDue = false;
            writeCommonHeader(header, sendNext);
            header.write(segment);
            return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return "StreamSession(" + key + ", " + state + ")";
    }

    // --------------------------------------------------------------------- //

    private void onSegmentInSynReceived(final TcpHeader header) {
        if (header.syn && !header.ack) {
            // First SYN, or a retransmission of it.
            if (!synReceived) {
                synReceived = true;
                receiveNext = header.sequenceNumber + 1;
                sendWindow = header.window;
                sendWindowUpdateSequence = header.sequenceNumber;
                sendWindowUpdateAcknowledgment = sendNext;
                if (header.maxSegmentSize > 0) {
                    peerMaxSegmentSize = Math.clamp(header.maxSegmentSize, MIN_PEER_MAX_SEGMENT_SIZE, DEFAULT_MAX_SEGMENT_SIZE);
                }
            } else if (socketConnected) {
                lastTransmitTime = now - retransmissionTimeoutNanos;
            }
            return;
        }

        if (header.ack
            && sendNext != initialSendSequence
            && TcpSequence.between(header.acknowledgmentNumber, initialSendSequence + 1, sendNext + 1)) {
            synAcked = true;
            sendUnacknowledged = initialSendSequence + 1;
            state = TcpState.ESTABLISHED;
            clearRetransmitTimer();
            sampleRoundTrip(header.acknowledgmentNumber);
            updateSendWindow(header);
        }
    }

    private boolean processAcknowledgment(final TcpHeader header) {
        final int acknowledgmentNumber = header.acknowledgmentNumber;
        if (TcpSequence.gt(acknowledgmentNumber, sendNext)) {
            // Acknowledges something we never sent; answer with the true state and drop it.
            ackDue = true;
            return false;
        }
        if (TcpSequence.leq(acknowledgmentNumber, sendUnacknowledged)) {
            // Duplicate acknowledgment. Nothing to advance, but the segment is still valid.
            return true;
        }

        sampleRoundTrip(acknowledgmentNumber);

        sendUnacknowledged = acknowledgmentNumber;
        retransmissions = 0;

        if (finSent && TcpSequence.gt(sendUnacknowledged, finSequence)) {
            finAcked = true;
        }

        final int dropped = Math.clamp(acknowledgmentNumber - dataBase, 0, toGuest.position());
        if (dropped > 0) {
            toGuest.flip();
            toGuest.position(dropped);
            toGuest.compact();
            dataBase += dropped;
        }

        if (TcpSequence.geq(sendUnacknowledged, sendNext)) {
            clearRetransmitTimer();
        } else {
            lastTransmitTime = now;
        }

        return true;
    }

    private void updateSendWindow(final TcpHeader header) {
        if (TcpSequence.lt(sendWindowUpdateSequence, header.sequenceNumber)
            || (sendWindowUpdateSequence == header.sequenceNumber
            && TcpSequence.leq(sendWindowUpdateAcknowledgment, header.acknowledgmentNumber))) {
            if (sendWindow != 0 && header.window == 0) {
                persistSince = now;
            }
            sendWindow = header.window;
            sendWindowUpdateSequence = header.sequenceNumber;
            sendWindowUpdateAcknowledgment = header.acknowledgmentNumber;
        }
    }

    private void processPayload(final TcpHeader header, final ByteBuffer payload) {
        int length = payload.remaining();
        if (length == 0) {
            return;
        }

        final int segmentStart = header.sequenceNumber;
        if (TcpSequence.lt(segmentStart, receiveNext)) {
            // Overlaps data we already took; skip the part we have.
            final int alreadyHave = receiveNext - segmentStart;
            if (alreadyHave >= length) {
                ackDue = true;
                return;
            }
            payload.position(payload.position() + alreadyHave);
            length -= alreadyHave;
        } else if (segmentStart != receiveNext) {
            // Arrived out of order. Say what we are actually waiting for and let the guest resend.
            ackDue = true;
            return;
        }

        final int accepted = Math.min(length, freeSpaceForGuestData());
        if (accepted > 0) {
            appendToRemote(payload, accepted);
            receiveNext += accepted;
        }
        // Acknowledge even when the window was full, so the guest learns the window is closed.
        ackDue = true;
    }

    private void processFin(final TcpHeader header, final int segmentEnd) {
        if (!header.fin || guestFin) {
            return;
        }
        if (receiveNext != segmentEnd) {
            ackDue = true;
            return;
        }
        guestFin = true;
        receiveNext += 1;
        ackDue = true;
        state = switch (state) {
            case ESTABLISHED -> TcpState.CLOSE_WAIT;
            case FIN_WAIT_1 -> TcpState.CLOSING;
            case FIN_WAIT_2 -> TcpState.CLOSED;
            default -> state;
        };
    }

    private void checkCloseHandshake() {
        if (finAcked) {
            state = switch (state) {
                case FIN_WAIT_1 -> TcpState.FIN_WAIT_2;
                case CLOSING, LAST_ACK -> TcpState.CLOSED;
                default -> state;
            };
        }
    }

    private int freeSpaceForGuestData() {
        return toRemote.capacity() - toRemote.remaining();
    }

    private void appendToRemote(final ByteBuffer payload, final int count) {
        final int oldLimit = payload.limit();
        payload.limit(payload.position() + count);
        toRemote.compact();
        toRemote.put(payload);
        toRemote.flip();
        payload.limit(oldLimit);
    }

    private boolean writeSynAck(final TcpHeader header, final ByteBuffer segment, final long now) {
        header.syn = true;
        header.ack = true;
        header.sequenceNumber = initialSendSequence;
        header.acknowledgmentNumber = receiveNext;
        header.window = advertiseWindow();
        header.maxSegmentSize = DEFAULT_MAX_SEGMENT_SIZE;
        header.write(segment);
        if (sendNext == initialSendSequence) {
            sendNext = initialSendSequence + 1;
        }
        armRetransmitTimer(now, initialSendSequence);
        ackDue = false;
        return true;
    }

    private boolean writeData(final TcpHeader header, final ByteBuffer segment, final int length, final long now) {
        probeDue = false;
        writeCommonHeader(header, sendNext);
        header.psh = length == unsentLength();
        header.write(segment);

        final int offset = sendNext - dataBase;
        if (offset < 0 || offset + length > toGuest.position()) {
            // Should be unreachable; refusing beats corrupting the stream or throwing on the
            // shared internet thread.
            LOGGER.warn("Refusing out of range segment on {} (offset {}, length {}).", key, offset, length);
            return false;
        }
        final int oldLimit = toGuest.limit();
        final int oldPosition = toGuest.position();
        toGuest.limit(offset + length);
        toGuest.position(offset);
        segment.put(toGuest);
        toGuest.limit(oldLimit);
        toGuest.position(oldPosition);

        armRetransmitTimer(now, sendNext);
        sendNext += length;
        ackDue = false;
        return true;
    }

    private boolean writeFin(final TcpHeader header, final ByteBuffer segment, final long now) {
        writeCommonHeader(header, sendNext);
        header.fin = true;
        header.write(segment);
        finSequence = sendNext;
        finSent = true;
        armRetransmitTimer(now, sendNext);
        sendNext += 1;
        ackDue = false;
        state = switch (state) {
            case ESTABLISHED -> TcpState.FIN_WAIT_1;
            case CLOSE_WAIT -> TcpState.LAST_ACK;
            default -> state;
        };
        return true;
    }

    private void writeCommonHeader(final TcpHeader header, final int sequenceNumber) {
        header.ack = true;
        header.sequenceNumber = sequenceNumber;
        header.acknowledgmentNumber = receiveNext;
        header.window = advertiseWindow();
    }

    private int computeWindow() {
        final int free = freeSpaceForGuestData();
        return free < Math.min(peerMaxSegmentSize, toRemote.capacity() / 2) ? 0 : Math.min(free, 0xFFFF);
    }

    private int advertiseWindow() {
        lastAdvertisedWindow = computeWindow();
        return lastAdvertisedWindow;
    }

    private int unsentLength() {
        return Math.max(0, toGuest.position() - (sendNext - dataBase));
    }

    private int sendableDataLength() {
        if (!synAcked || finSent) {
            return 0;
        }
        final int unsent = unsentLength();
        if (unsent <= 0) {
            return 0;
        }
        final int inFlight = sendNext - sendUnacknowledged;
        final int usableWindow = sendWindow - inFlight;
        if (usableWindow <= 0) {
            return inFlight == 0 && (probeDue || isPersistDue()) ? 1 : 0;
        }
        return Math.min(Math.min(unsent, usableWindow), peerMaxSegmentSize);
    }

    private boolean isFinDue() {
        if (!remoteEof || finSent || !synAcked || unsentLength() != 0) {
            return false;
        }
        return switch (state) {
            case ESTABLISHED, CLOSE_WAIT, FIN_WAIT_1, CLOSING, LAST_ACK -> true;
            default -> false;
        };
    }

    private boolean isPersistDue() {
        return now - lastTransmitTime >= retransmissionTimeoutNanos;
    }

    private boolean isSynAckDue(final long now) {
        return sendNext == initialSendSequence || isRetransmitDue(now);
    }

    private boolean isRetransmitDue(final long now) {
        return TcpSequence.lt(sendUnacknowledged, sendNext) && now - lastTransmitTime >= retransmissionTimeoutNanos;
    }

    private void armRetransmitTimer(final long now, final int sequenceNumber) {
        lastTransmitTime = now;
        if (!hasRoundTripProbe) {
            roundTripProbeSequence = sequenceNumber;
            roundTripProbeTime = now;
            hasRoundTripProbe = true;
        }
    }

    private void clearRetransmitTimer() {
        lastTransmitTime = now;
        retransmissions = 0;
    }

    private void backOffRetransmitTimer() {
        lastTransmitTime = now;
        retransmissionTimeoutNanos = Math.min(retransmissionTimeoutNanos * 2,
            TimeUnit.MILLISECONDS.toNanos(config.maxRetransmissionTimeoutMs()));
    }

    private void sampleRoundTrip(final int acknowledgmentNumber) {
        if (!hasRoundTripProbe || !TcpSequence.gt(acknowledgmentNumber, roundTripProbeSequence)) {
            return;
        }
        if (hasRetransmitted && !TcpSequence.gt(acknowledgmentNumber, noRoundTripSampleBefore)) {
            // Ambiguous: this could be acknowledging either copy.
            return;
        }
        hasRetransmitted = false;
        hasRoundTripProbe = false;

        final long sample = now - roundTripProbeTime;
        if (smoothedRoundTripTimeNanos == 0) {
            smoothedRoundTripTimeNanos = sample;
            roundTripTimeVarianceNanos = sample / 2;
        } else {
            roundTripTimeVarianceNanos = (3 * roundTripTimeVarianceNanos + Math.abs(smoothedRoundTripTimeNanos - sample)) / 4;
            smoothedRoundTripTimeNanos = (7 * smoothedRoundTripTimeNanos + sample) / 8;
        }

        final long minRetransmissionTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(config.initialRetransmissionTimeoutMs());
        final long maxRetransmissionTimeoutNanos = TimeUnit.MILLISECONDS.toNanos(config.maxRetransmissionTimeoutMs());
        retransmissionTimeoutNanos = Math.clamp(
            smoothedRoundTripTimeNanos + 4 * roundTripTimeVarianceNanos,
            minRetransmissionTimeoutNanos, maxRetransmissionTimeoutNanos);
    }

    // --------------------------------------------------------------------- //

    private enum TcpState {
        SYN_RECEIVED(SessionState.NEW),
        ESTABLISHED(SessionState.ESTABLISHED),
        FIN_WAIT_1(SessionState.ESTABLISHED),
        FIN_WAIT_2(SessionState.ESTABLISHED),
        CLOSE_WAIT(SessionState.ESTABLISHED),
        CLOSING(SessionState.ESTABLISHED),
        LAST_ACK(SessionState.ESTABLISHED),
        CLOSED(SessionState.FINISH),
        EXPIRED(SessionState.EXPIRED);

        private final SessionState sessionState;

        TcpState(final SessionState sessionState) {
            this.sessionState = sessionState;
        }

        SessionState toSessionState() {
            return sessionState;
        }
    }
}

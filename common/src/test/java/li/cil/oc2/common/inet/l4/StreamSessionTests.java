/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class StreamSessionTests {
    private static final int BUFFER_SIZE = 4096;
    private static final int RTO_MS = 250;

    private static final SessionKey.Stream KEY =
        new SessionKey.Stream(0x0A000002, (short) 40000, 0x08080808, (short) 80);

    private StreamSession session;
    private final TcpHeader header = new TcpHeader();
    private long now;
    private int guestSequence;
    private int sessionSequence;

    @BeforeEach
    public void setUp() {
        session = new StreamSession(KEY,
            new StreamSession.TcpConfig(BUFFER_SIZE, RTO_MS, 8000));
        now = TimeUnit.SECONDS.toNanos(1);
        guestSequence = 0x1000;
    }

    // --------------------------------------------------------------------- //

    @Test
    public void handshakeWaitsForTheHostSocket() {
        establish();
    }

    @Test
    public void connectionRefusedBeforeTheHandshakeSendsReset() {
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        session.close();

        final Segment reset = expectSegment();
        assertTrue(reset.header().rst);
        assertTrue(session.isFinished());
    }

    @Test
    public void repeatedSynResendsTheSynAck() {
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        session.connect();
        final Segment first = expectSegment();
        expectNothingToSend();

        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        final Segment second = expectSegment();

        assertTrue(second.header().syn);
        assertTrue(second.header().ack);
        assertEquals(first.header().sequenceNumber, second.header().sequenceNumber);
    }

    @Test
    public void segmentHalfTheSequenceSpaceAwayIsDroppedWithAnAck() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 0x80000000, sessionSequence, 8192, new byte[10]);

        final Segment ack = expectSegment();
        assertTrue(ack.header().ack);
        assertEquals(guestSequence, ack.header().acknowledgmentNumber);
        assertEquals(0, ack.payload().length);
    }

    @Test
    public void peerMaximumSegmentSizeIsHonoured() {
        final TcpHeader syn = new TcpHeader();
        syn.clear();
        syn.syn = true;
        syn.sequenceNumber = guestSequence;
        syn.window = 8192;
        syn.maxSegmentSize = 536;
        session.onSegment(syn, ByteBuffer.allocate(0), now);
        session.connect();

        final Segment synAck = expectSegment();
        sessionSequence = synAck.header().sequenceNumber + 1;
        guestSequence += 1;
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192, new byte[0]);

        fromRemote("x".repeat(2000));
        final Segment data = expectSegment();
        assertEquals(536, data.payload().length);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void dataFromTheGuestReachesTheHostSocket() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK | TcpHeader.FLAG_PSH, guestSequence, sessionSequence, 8192,
            "hello".getBytes(StandardCharsets.UTF_8));

        assertEquals("hello", drainToRemote());

        final Segment ack = expectSegment();
        assertTrue(ack.header().ack);
        assertEquals(guestSequence + 5, ack.header().acknowledgmentNumber);
    }

    @Test
    public void dataFromTheHostSocketReachesTheGuest() {
        establish();
        fromRemote("world");

        final Segment data = expectSegment();
        assertEquals("world", data.text());
        assertEquals(sessionSequence, data.header().sequenceNumber);
        assertTrue(data.header().psh, "the last segment of available data should push");

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 5, 8192, new byte[0]);
        expectNothingToSend();
    }

    @Test
    public void largeTransfersAreSplitAtTheSegmentSize() {
        establish();
        final int total = StreamSession.DEFAULT_MAX_SEGMENT_SIZE + 100;
        fromRemote("y".repeat(total));

        final Segment first = expectSegment();
        assertEquals(StreamSession.DEFAULT_MAX_SEGMENT_SIZE, first.payload().length);
        assertFalse(first.header().psh, "more data is still queued");

        final Segment second = expectSegment();
        assertEquals(100, second.payload().length);
        assertTrue(second.header().psh);
        assertEquals(sessionSequence + StreamSession.DEFAULT_MAX_SEGMENT_SIZE, second.header().sequenceNumber);
    }

    @Test
    public void sendingStopsAtTheAdvertisedWindow() {
        establish();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 10, new byte[0]);
        fromRemote("z".repeat(100));

        final Segment first = expectSegment();
        assertEquals(10, first.payload().length);
        expectNothingToSend();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 10, 100, new byte[0]);
        final Segment second = expectSegment();
        assertEquals(90, second.payload().length);
    }

    @Test
    public void closedWindowIsProbedRatherThanDeadlocked() {
        establish();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 0, new byte[0]);
        fromRemote("probe me");

        expectNothingToSend();

        advance(RTO_MS + 1);
        final Segment probe = expectSegment();
        assertEquals(1, probe.payload().length);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void unacknowledgedDataIsRetransmitted() {
        establish();
        fromRemote("retry");

        final Segment first = expectSegment();
        assertEquals("retry", first.text());
        expectNothingToSend();

        advance(RTO_MS + 1);
        final Segment again = expectSegment();
        assertEquals("retry", again.text());
        assertEquals(first.header().sequenceNumber, again.header().sequenceNumber);
    }

    @Test
    public void retransmissionBacksOff() {
        establish();
        fromRemote("retry");
        expectSegment();

        advance(RTO_MS + 1);
        expectSegment();

        advance(RTO_MS + 1);
        expectNothingToSend();

        advance(RTO_MS + 1);
        expectSegment();
    }

    @Test
    public void connectionThatNeverAcknowledgesIsReset() {
        establish();
        fromRemote("gone");

        boolean sawReset = false;
        for (int i = 0; i < 20 && !sawReset; ++i) {
            final Segment segment = toGuest();
            if (segment != null && segment.header().rst) {
                sawReset = true;
                break;
            }
            advance(30000);
        }

        assertTrue(sawReset, "a peer that never acknowledges should eventually be given up on");
        assertTrue(session.isFinished());
    }

    @Test
    public void outOfOrderDataIsRefusedButAcknowledged() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 10, sessionSequence, 8192,
            "future".getBytes(StandardCharsets.UTF_8));

        assertEquals("", drainToRemote(), "a gap must not be filled with later data");

        final Segment ack = expectSegment();
        assertTrue(ack.header().ack);
        assertEquals(guestSequence, ack.header().acknowledgmentNumber,
            "the acknowledgment should still point at the missing byte");
    }

    @Test
    public void retransmissionOverlappingKnownDataIsTrimmed() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192,
            "abcde".getBytes(StandardCharsets.UTF_8));
        assertEquals("abcde", drainToRemote());
        expectSegment();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 2, sessionSequence, 8192,
            "cdefg".getBytes(StandardCharsets.UTF_8));
        assertEquals("fg", drainToRemote(), "only the genuinely new bytes should be taken");
    }

    @Test
    public void duplicateAcknowledgmentsDoNotDisturbTheConnection() {
        establish();
        fromRemote("data");
        final Segment sent = expectSegment();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 4, 8192, new byte[0]);
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 4, 8192, new byte[0]);
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 4, 8192, new byte[0]);

        assertEquals(SessionState.ESTABLISHED, session.getState());
        expectNothingToSend();
        assertEquals(4, sent.payload().length);
    }

    @Test
    public void acknowledgmentForUnsentDataIsRefused() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 5000, 8192,
            "ignored".getBytes(StandardCharsets.UTF_8));

        assertEquals("", drainToRemote());
        final Segment ack = expectSegment();
        assertEquals(guestSequence, ack.header().acknowledgmentNumber);
        assertEquals(SessionState.ESTABLISHED, session.getState());
    }

    // --------------------------------------------------------------------- //

    @Test
    public void guestFinIsAcknowledgedAndHalfClosesTheStream() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK | TcpHeader.FLAG_FIN, guestSequence, sessionSequence, 8192,
            "bye".getBytes(StandardCharsets.UTF_8));

        assertEquals("bye", drainToRemote());
        assertTrue(session.isGuestHalfClosed());

        final Segment ack = expectSegment();
        assertTrue(ack.header().ack);
        assertEquals(guestSequence + 4, ack.header().acknowledgmentNumber,
            "the FIN occupies one sequence number after the payload");
    }

    @Test
    public void remoteEndOfStreamSendsFinAfterTheRemainingData() {
        establish();
        fromRemote("tail");
        session.finishReceiving();

        final Segment data = expectSegment();
        assertEquals("tail", data.text());
        assertFalse(data.header().fin, "the FIN must wait until the data is out");

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 4, 8192, new byte[0]);

        final Segment fin = expectSegment();
        assertTrue(fin.header().fin);
        assertEquals(sessionSequence + 4, fin.header().sequenceNumber);

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 5, 8192, new byte[0]);
        assertEquals(SessionState.ESTABLISHED, session.getState());

        fromGuest(TcpHeader.FLAG_ACK | TcpHeader.FLAG_FIN, guestSequence, sessionSequence + 5, 8192, new byte[0]);
        expectSegment();
        assertTrue(session.isFinished());
    }

    @Test
    public void bothSidesClosingLeavesTheSessionFinished() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK | TcpHeader.FLAG_FIN, guestSequence, sessionSequence, 8192, new byte[0]);
        expectSegment();

        session.finishReceiving();
        final Segment fin = expectSegment();
        assertTrue(fin.header().fin);

        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 1, fin.header().sequenceNumber + 1, 8192, new byte[0]);
        assertTrue(session.isFinished());
    }

    @Test
    public void resetFromTheGuestClosesImmediately() {
        establish();

        fromGuest(TcpHeader.FLAG_RST, guestSequence, sessionSequence, 8192, new byte[0]);

        assertTrue(session.isFinished());
        expectNothingToSend();
    }

    @Test
    public void synOnAnEstablishedConnectionResetsIt() {
        establish();

        fromGuest(TcpHeader.FLAG_SYN, guestSequence + 100, 0, 8192, new byte[0]);

        final Segment reset = expectSegment();
        assertTrue(reset.header().rst);
        assertTrue(session.isFinished());
    }

    @Test
    public void expiryResetsAnEstablishedConnection() {
        establish();

        session.expire();

        assertEquals(SessionState.EXPIRED, session.getState());
        final Segment reset = expectSegment();
        assertTrue(reset.header().rst);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void acknowledgmentBeforeTheSynAckIsIgnored() {
        for (int attempt = 0; attempt < 64; ++attempt) {
            setUp();
            fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);

            fromGuest(TcpHeader.FLAG_ACK, guestSequence + 1, attempt * 0x04000000, 8192, new byte[0]);

            assertEquals(SessionState.NEW, session.getState(),
                "an unsolicited acknowledgment must not establish the connection");
            expectNothingToSend();

            session.connect();
            final Segment synAck = expectSegment();
            assertTrue(synAck.header().syn);
        }
    }

    @Test
    public void acknowledgmentBeyondWhatWeSentIsIgnored() {
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        session.connect();
        final Segment synAck = expectSegment();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 1, synAck.header().sequenceNumber + 5000, 8192, new byte[0]);

        assertEquals(SessionState.NEW, session.getState());
    }

    @Test
    public void lostFinIsRetransmitted() {
        establish();
        session.finishReceiving();

        final Segment fin = expectSegment();
        assertTrue(fin.header().fin);
        expectNothingToSend();

        advance(RTO_MS + 1);
        final Segment again = expectSegment();
        assertTrue(again.header().fin, "the FIN must go out again");
        assertEquals(fin.header().sequenceNumber, again.header().sequenceNumber);

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, again.header().sequenceNumber + 1, 8192, new byte[0]);
        assertEquals(SessionState.ESTABLISHED, session.getState());
    }

    @Test
    public void finLostRepeatedlyStillEventuallyArrives() {
        establish();
        session.finishReceiving();

        int finsSeen = 0;
        for (int i = 0; i < 5; ++i) {
            final Segment segment = expectSegment();
            if (segment.header().fin) {
                ++finsSeen;
            }
            advance(30000);
        }

        assertTrue(finsSeen >= 3, "a FIN that is never acknowledged should keep being retransmitted");
    }

    @Test
    public void reopeningTheWindowIsAnnouncedToTheGuest() {
        establish();

        final int capacity = session.getSendBuffer().capacity();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192, new byte[capacity]);
        guestSequence += capacity;

        final Segment shut = expectSegment();
        assertEquals(0, shut.header().window, "the window should be closed once the buffer is full");
        expectNothingToSend();

        drainToRemote();

        final Segment reopened = expectSegment();
        assertTrue(reopened.header().window > 0, "the reopened window must be announced unprompted");
        assertTrue(reopened.header().ack);
    }

    @Test
    public void peerHoldingTheWindowShutIsNotResetForIt() {
        establish();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 0, new byte[0]);
        fromRemote("waiting on you");

        for (int i = 0; i < 20; ++i) {
            advance(1000);
            final Segment segment = toGuest();
            if (segment != null) {
                assertFalse(segment.header().rst, "a closed window must not be treated as a failure");
                assertEquals(1, segment.payload().length, "the probe should stay one byte");
            }
        }

        assertEquals(SessionState.ESTABLISHED, session.getState());
    }

    @Test
    public void windowHeldShutForeverIsEventuallyGivenUpOn() {
        establish();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 0, new byte[0]);
        fromRemote("waiting on you");

        boolean sawReset = false;
        for (int i = 0; i < 40 && !sawReset; ++i) {
            advance(10000);
            final Segment segment = toGuest();
            sawReset = segment != null && segment.header().rst;
        }

        assertTrue(sawReset, "a window held shut indefinitely should end the connection");
        assertTrue(session.isFinished());
    }

    // --------------------------------------------------------------------- //

    @Test
    public void sequenceNumbersWrapWithoutStallingTheConnection() {
        guestSequence = Integer.MAX_VALUE - 2;
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192,
            "abcdef".getBytes(StandardCharsets.UTF_8));

        assertEquals("abcdef", drainToRemote(), "data was refused across the sequence wrap");

        final Segment ack = expectSegment();
        assertEquals(guestSequence + 6, ack.header().acknowledgmentNumber);
    }

    // --------------------------------------------------------------------- //

    private void advance(final long millis) {
        now += TimeUnit.MILLISECONDS.toNanos(millis);
    }

    private void fromGuest(final int flags, final int sequenceNumber, final int acknowledgmentNumber,
                           final int window, final byte[] payload) {
        final TcpHeader inbound = new TcpHeader();
        inbound.clear();
        inbound.sequenceNumber = sequenceNumber;
        inbound.acknowledgmentNumber = acknowledgmentNumber;
        inbound.syn = (flags & TcpHeader.FLAG_SYN) != 0;
        inbound.ack = (flags & TcpHeader.FLAG_ACK) != 0;
        inbound.fin = (flags & TcpHeader.FLAG_FIN) != 0;
        inbound.rst = (flags & TcpHeader.FLAG_RST) != 0;
        inbound.psh = (flags & TcpHeader.FLAG_PSH) != 0;
        inbound.window = window;
        session.onSegment(inbound, ByteBuffer.wrap(payload), now);
    }

    @Nullable
    private Segment toGuest() {
        if (!session.wantsToSend(now)) {
            return null;
        }
        final ByteBuffer buffer = ByteBuffer.allocate(2048);
        if (!session.writeSegment(header, buffer, now)) {
            return null;
        }
        buffer.flip();

        final TcpHeader parsed = new TcpHeader();
        assertTrue(parsed.read(buffer), "session emitted a segment its own parser rejects");
        final byte[] payload = new byte[buffer.remaining()];
        buffer.get(payload);
        return new Segment(parsed, payload);
    }

    private Segment expectSegment() {
        final Segment segment = toGuest();
        assertNotNull(segment, "expected the session to have something to send");
        return segment;
    }

    private void expectNothingToSend() {
        assertNull(toGuest(), "expected the session to have nothing to send");
    }

    private void establish() {
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        expectNothingToSend();

        session.connect();
        final Segment synAck = expectSegment();
        assertTrue(synAck.header().syn);
        assertTrue(synAck.header().ack);
        assertEquals(guestSequence + 1, synAck.header().acknowledgmentNumber);
        assertEquals(StreamSession.DEFAULT_MAX_SEGMENT_SIZE, synAck.header().maxSegmentSize);

        sessionSequence = synAck.header().sequenceNumber + 1;
        guestSequence += 1;

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192, new byte[0]);
        assertEquals(SessionState.ESTABLISHED, session.getState());
    }

    private void fromRemote(final String text) {
        session.getReceiveBuffer().put(text.getBytes(StandardCharsets.UTF_8));
    }

    private String drainToRemote() {
        final ByteBuffer buffer = session.getSendBuffer();
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private record Segment(TcpHeader header, byte[] payload) {
        String text() {
            return new String(payload, StandardCharsets.UTF_8);
        }
    }
}

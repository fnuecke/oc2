/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

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
    // Handshake

    @Test
    public void handshakeWaitsForTheHostSocket() {
        establish();
    }

    @Test
    public void connectionRefusedBeforeTheHandshakeSendsReset() {
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        session.close(); // What the session layer does when the host socket is refused.

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

        // The guest never saw it and asks again.
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        final Segment second = expectSegment();

        assertTrue(second.header().syn);
        assertTrue(second.header().ack);
        assertEquals(first.header().sequenceNumber, second.header().sequenceNumber);
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

        // Acknowledging and reopening the window lets the rest through.
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 10, 100, new byte[0]);
        final Segment second = expectSegment();
        assertEquals(90, second.payload().length);
    }

    @Test
    public void aClosedWindowIsProbedRatherThanDeadlocked() {
        establish();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 0, new byte[0]);
        fromRemote("probe me");

        // Nothing may go out while the window is shut.
        expectNothingToSend();

        // Once the retransmission timer fires, a single byte goes out to draw a window update.
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

        // The timeout has doubled, so the original interval is no longer enough.
        advance(RTO_MS + 1);
        expectNothingToSend();

        advance(RTO_MS + 1);
        expectSegment();
    }

    @Test
    public void aConnectionThatNeverAcknowledgesIsReset() {
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

        // A segment from the future, as if the one before it was lost.
        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 10, sessionSequence, 8192,
            "future".getBytes(StandardCharsets.UTF_8));

        assertEquals("", drainToRemote(), "a gap must not be filled with later data");

        final Segment ack = expectSegment();
        assertTrue(ack.header().ack);
        assertEquals(guestSequence, ack.header().acknowledgmentNumber,
            "the acknowledgment should still point at the missing byte");
    }

    @Test
    public void aRetransmissionOverlappingKnownDataIsTrimmed() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192,
            "abcde".getBytes(StandardCharsets.UTF_8));
        assertEquals("abcde", drainToRemote());
        expectSegment();

        // The guest missed our acknowledgment and resends from three bytes back.
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
    public void anAcknowledgmentForUnsentDataIsRefused() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence + 5000, 8192,
            "ignored".getBytes(StandardCharsets.UTF_8));

        assertEquals("", drainToRemote());
        final Segment ack = expectSegment();
        assertEquals(guestSequence, ack.header().acknowledgmentNumber);
        assertEquals(SessionState.ESTABLISHED, session.getState());
    }

    // --------------------------------------------------------------------- //
    // Shutdown

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

        // The guest closes its half too, finishing the exchange.
        fromGuest(TcpHeader.FLAG_ACK | TcpHeader.FLAG_FIN, guestSequence, sessionSequence + 5, 8192, new byte[0]);
        expectSegment();
        assertTrue(session.isFinished());
    }

    @Test
    public void bothSidesClosingLeavesTheSessionFinished() {
        establish();

        fromGuest(TcpHeader.FLAG_ACK | TcpHeader.FLAG_FIN, guestSequence, sessionSequence, 8192, new byte[0]);
        expectSegment(); // Our acknowledgment of the guest's FIN.

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
    // Regressions

    /**
     * A guest that acknowledges a SYN-ACK we never sent must not be believed. Taking it on trust
     * moved the send pointer behind the buffer's base and drove a negative buffer index, which
     * poisoned the session and stalled every other session on the same stack.
     */
    @Test
    public void anAcknowledgmentBeforeTheSynAckIsIgnored() {
        // Run the whole space of initial sequence numbers the session might have picked, since the
        // original defect only bit for roughly half of them.
        for (int attempt = 0; attempt < 64; ++attempt) {
            setUp();
            fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);

            // The host socket has not connected, so no SYN-ACK has gone out yet.
            fromGuest(TcpHeader.FLAG_ACK, guestSequence + 1, attempt * 0x04000000, 8192, new byte[0]);

            assertEquals(SessionState.NEW, session.getState(),
                "an unsolicited acknowledgment must not establish the connection");
            expectNothingToSend();

            // The real handshake still works afterwards.
            session.connect();
            final Segment synAck = expectSegment();
            assertTrue(synAck.header().syn);
        }
    }

    @Test
    public void anAcknowledgmentBeyondWhatWeSentIsIgnored() {
        fromGuest(TcpHeader.FLAG_SYN, guestSequence, 0, 8192, new byte[0]);
        session.connect();
        final Segment synAck = expectSegment();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence + 1, synAck.header().sequenceNumber + 5000, 8192, new byte[0]);

        assertEquals(SessionState.NEW, session.getState());
    }

    /**
     * A lost FIN has to be resent. Retransmission clears the sent flag but leaves the state where
     * the FIN put it, so the state machine has to allow the resend from there too.
     */
    @Test
    public void aLostFinIsRetransmitted() {
        establish();
        session.finishReceiving();

        final Segment fin = expectSegment();
        assertTrue(fin.header().fin);
        expectNothingToSend();

        // The guest never saw it.
        advance(RTO_MS + 1);
        final Segment again = expectSegment();
        assertTrue(again.header().fin, "the FIN must go out again");
        assertEquals(fin.header().sequenceNumber, again.header().sequenceNumber);

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, again.header().sequenceNumber + 1, 8192, new byte[0]);
        assertEquals(SessionState.ESTABLISHED, session.getState());
    }

    @Test
    public void aFinLostRepeatedlyStillEventuallyArrives() {
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

    /**
     * A window that reopens has to be announced. The guest stops sending on a zero window and will
     * not retry until its own persist timer fires, which backs off into tens of seconds.
     */
    @Test
    public void reopeningTheWindowIsAnnouncedToTheGuest() {
        establish();

        // Fill the buffer the guest sends into, so the window we advertise closes.
        final int capacity = session.getSendBuffer().capacity();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192, new byte[capacity]);
        guestSequence += capacity;

        final Segment shut = expectSegment();
        assertEquals(0, shut.header().window, "the window should be closed once the buffer is full");
        expectNothingToSend();

        // The session layer drains it to the host socket.
        drainToRemote();

        final Segment reopened = expectSegment();
        assertTrue(reopened.header().window > 0, "the reopened window must be announced unprompted");
        assertTrue(reopened.header().ack);
    }

    /**
     * A peer holding its window shut is not a broken peer, so probing it must not count toward
     * giving up on the connection (RFC 1122 4.2.2.17).
     */
    @Test
    public void aPeerHoldingTheWindowShutIsNotResetForIt() {
        establish();
        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 0, new byte[0]);
        fromRemote("waiting on you");

        // Well past MAX_RETRANSMISSIONS worth of probes, but inside the persist timeout.
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

    /**
     * It must not probe forever either: a guest advertising a zero window and answering one probe a
     * minute would otherwise pin a session slot, a host socket and its buffers for good.
     */
    @Test
    public void aWindowHeldShutForeverIsEventuallyGivenUpOn() {
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
    // Sequence wrapping

    @Test
    public void sequenceNumbersWrapWithoutStallingTheConnection() {
        // Start the guest just below the wrap so its numbering rolls over mid-transfer.
        guestSequence = Integer.MAX_VALUE - 2;
        establish();

        fromGuest(TcpHeader.FLAG_ACK, guestSequence, sessionSequence, 8192,
            "abcdef".getBytes(StandardCharsets.UTF_8));

        assertEquals("abcdef", drainToRemote(),
            "data must still be accepted across the sequence number wrap");

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
        // Nothing goes out until the host socket is up.
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

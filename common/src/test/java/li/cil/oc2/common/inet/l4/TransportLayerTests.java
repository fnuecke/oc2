/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import li.cil.oc2.common.inet.l2.LinkLocalLayer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class TransportLayerTests {
    private static final int GUEST_IP = 0x0A000002;
    private static final int REMOTE_IP = 0x08080808;
    private static final short GUEST_PORT = (short) 40000;
    private static final short REMOTE_PORT = 80;
    private static final int MESSAGE_START = 20;

    private ScriptedSessionLayer sessionLayer;
    private TransportLayer transport;
    private int guestSequence = 0x5000;

    @BeforeEach
    public void setUp() {
        sessionLayer = new ScriptedSessionLayer();
        transport = newTransport(new TokenBucket(1024, 1024));
    }

    // --------------------------------------------------------------------- //

    @Test
    public void aStreamSegmentStartsAtTheStartOfTheMessage() {
        final StreamSession stream = establishStream();
        stream.getReceiveBuffer().put("hello".getBytes(StandardCharsets.UTF_8));

        final Received received = receive();
        assertEquals(TransportLayer.PROTOCOL_TCP, received.protocol());
        parse(received.buffer());
    }

    @Test
    public void aWithdrawnClaimLeavesTheMessageBufferUntouched() {
        sendUdp();
        final AbstractSession datagram = sessionLayer.lastDatagram;
        assertNotNull(datagram, "the transport layer should have opened a datagram session");

        final StreamSession stream = establishStream();
        stream.getReceiveBuffer().put("payload".getBytes(StandardCharsets.UTF_8));

        sessionLayer.script = receiver -> {
            receiver.receive(datagram);
            receiver.cancel();
        };

        final Received received = receive();
        assertEquals(TransportLayer.PROTOCOL_TCP, received.protocol(),
            "the stream should still be served after the withdrawn claim");

        final TcpHeader header = parse(received.buffer());
        final byte[] payload = new byte[received.buffer().remaining()];
        received.buffer().get(payload);
        assertEquals("payload", new String(payload, StandardCharsets.UTF_8));
        assertTrue(header.ack);
    }

    @Test
    public void aSegmentForAnUnknownConnectionIsAnsweredWithAReset() {
        sendTcp(TcpHeader.FLAG_ACK, guestSequence, 12345);

        assertNull(sessionLayer.lastStream, "a stray segment must not cost a session");

        final Received received = receive();
        assertEquals(TransportLayer.PROTOCOL_TCP, received.protocol());
        assertTrue(parse(received.buffer()).rst);
    }

    @Test
    public void straySegmentsCannotGrowTheResetQueueWithoutBound() {
        for (int i = 0; i < 200; ++i) {
            sendTcp(TcpHeader.FLAG_ACK, guestSequence + i, 12345);
        }

        int resets = 0;
        while (receive().protocol() != TransportLayer.PROTOCOL_NONE) {
            ++resets;
            assertTrue(resets <= 32, "the reset queue should be bounded, not hold every stray segment");
        }
        assertTrue(resets > 0, "at least one reset should still go out");
    }

    @Test
    public void newSessionsBeyondTheRateAreRefusedWithAReset() {
        final SessionLimits limits = new SessionLimits(8, 32);
        transport = newTransport(limits, new TokenBucket(2, 0));

        sendTcp((short) 40001, TcpHeader.FLAG_SYN, guestSequence, 0);
        sendTcp((short) 40002, TcpHeader.FLAG_SYN, guestSequence, 0);
        assertEquals(2, limits.getUsed());

        sessionLayer.lastStream = null;
        sendTcp((short) 40003, TcpHeader.FLAG_SYN, guestSequence, 0);
        assertNull(sessionLayer.lastStream, "the third connection must not be opened");
        assertEquals(2, limits.getUsed(), "a refused connection must not hold a slot");

        final Received received = receive();
        assertEquals(TransportLayer.PROTOCOL_TCP, received.protocol());
        received.buffer().getShort();
        assertEquals((short) 40003, received.buffer().getShort(), "the reset should go to the refused connection");
        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(received.buffer()));
        assertTrue(header.rst);
    }

    // --------------------------------------------------------------------- //

    private TransportLayer newTransport(final TokenBucket sessionRate) {
        return newTransport(new SessionLimits(8, 32), sessionRate);
    }

    private TransportLayer newTransport(final SessionLimits limits, final TokenBucket sessionRate) {
        return new TransportLayer(sessionLayer,
            new PortFilter(List.of()),
            limits, sessionRate, () -> 1,
            new StreamSession.TcpConfig(8192, 100, 1000),
            TimeUnit.SECONDS.toNanos(60));
    }

    private ByteBuffer newMessageBuffer() {
        final ByteBuffer buffer = ByteBuffer.allocate(LinkLocalLayer.FRAME_SIZE);
        buffer.position(MESSAGE_START);
        return buffer;
    }

    private void sendTcp(final int flags, final int sequenceNumber, final int acknowledgmentNumber) {
        sendTcp(GUEST_PORT, flags, sequenceNumber, acknowledgmentNumber);
    }

    private void sendTcp(final short sourcePort, final int flags, final int sequenceNumber, final int acknowledgmentNumber) {
        final ByteBuffer buffer = ByteBuffer.allocate(64);
        buffer.putShort(sourcePort);
        buffer.putShort(REMOTE_PORT);
        buffer.putInt(sequenceNumber);
        buffer.putInt(acknowledgmentNumber);
        buffer.put((byte) 0x50);
        buffer.put((byte) flags);
        buffer.putShort((short) 8192);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.flip();

        final TransportMessage message = new TransportMessage();
        message.initializeBuffer(buffer);
        message.updateIpv4(GUEST_IP, REMOTE_IP);
        transport.sendTransportMessage(TransportLayer.PROTOCOL_TCP, message);
    }

    private void sendUdp() {
        final ByteBuffer buffer = ByteBuffer.allocate(32);
        buffer.putShort(GUEST_PORT);
        buffer.putShort((short) 53);
        buffer.putShort((short) 12); // Length: header plus four bytes.
        buffer.putShort((short) 0);
        buffer.put("ping".getBytes(StandardCharsets.UTF_8));
        buffer.flip();

        final TransportMessage message = new TransportMessage();
        message.initializeBuffer(buffer);
        message.updateIpv4(GUEST_IP, REMOTE_IP);
        transport.sendTransportMessage(TransportLayer.PROTOCOL_UDP, message);
    }

    private StreamSession establishStream() {
        sendTcp(TcpHeader.FLAG_SYN, guestSequence, 0);
        final StreamSession stream = sessionLayer.lastStream;
        assertNotNull(stream, "the transport layer should have opened a stream session");
        stream.connect();

        final Received synAck = receive();
        assertEquals(TransportLayer.PROTOCOL_TCP, synAck.protocol());

        final TcpHeader header = parse(synAck.buffer());
        assertTrue(header.syn);
        guestSequence += 1;
        sendTcp(TcpHeader.FLAG_ACK, guestSequence, header.sequenceNumber + 1);
        return stream;
    }

    private Received receive() {
        final ByteBuffer buffer = newMessageBuffer();
        final TransportMessage message = new TransportMessage();
        message.initializeBuffer(buffer);
        final byte protocol = transport.receiveTransportMessage(message);
        return new Received(protocol, buffer);
    }

    private TcpHeader parse(final ByteBuffer buffer) {
        assertEquals(MESSAGE_START, buffer.position(),
            "the message must start where the network layer left room for it");
        assertEquals(REMOTE_PORT, buffer.getShort(), "source port is not where it should be");
        assertEquals(GUEST_PORT, buffer.getShort(), "destination port is not where it should be");
        final TcpHeader header = new TcpHeader();
        assertTrue(header.read(buffer), "the emitted header does not parse");
        return header;
    }

    private record Received(byte protocol, ByteBuffer buffer) {
    }

    private static final class ScriptedSessionLayer implements SessionLayer {
        @Nullable
        StreamSession lastStream;
        @Nullable
        AbstractSession lastDatagram;
        @Nullable
        Consumer<Receiver> script;

        @Override
        public void receiveSession(final Receiver receiver) {
            final Consumer<Receiver> script = this.script;
            if (script != null) {
                this.script = null;
                script.accept(receiver);
            }
        }

        @Override
        public void sendSession(final AbstractSession session, @Nullable final ByteBuffer data) {
            if (session instanceof final StreamSession stream) {
                lastStream = stream;
            } else if (session instanceof DatagramSession) {
                lastDatagram = session;
            }
        }
    }
}

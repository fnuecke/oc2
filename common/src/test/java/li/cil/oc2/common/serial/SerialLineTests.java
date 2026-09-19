/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.device.serial.UART16550A;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class SerialLineTests {
    private static final int UART_RBR_OFFSET = 0;
    private static final int UART_THR_OFFSET = 0;
    private static final int UART_FCR_OFFSET = 2;
    private static final int UART_LSR_OFFSET = 5;

    private static final int UART_LCR_OFFSET = 3;
    private static final int UART_DLL_OFFSET = 0;
    private static final int UART_DLM_OFFSET = 1;

    private static final int UART_FCR_FE = 1 << 0;
    private static final int UART_LCR_DLAB = 1 << 7;

    private static final int B300 = 300;
    private static final int B1200 = 1200;
    private static final int B9600 = 9600;
    private static final int B19200 = 19200;
    private static final int B115200 = 115200;
    private static final int UART_LSR_DR = 1 << 0;
    private static final int UART_LSR_FE = 1 << 3;

    // --------------------------------------------------------------------- //

    private long tick;
    private Endpoint a;
    private Endpoint b;

    @BeforeEach
    public void setUp() {
        tick = 1;
        a = new Endpoint(1, B9600);
        b = new Endpoint(2, B9600);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void everyEndpointOnTheSegmentHearsEveryOther() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("hi");
        segment(a, b, c);

        assertEquals("hi", b.read(), "the wire is multi-drop, not a pair of endpoints");
        assertEquals("hi", c.read());
        assertEquals("", a.read(), "and a endpoint does not hear itself");
    }

    @Test
    public void bytesSurviveTheRoundTrip() {
        a.write("the quick brown fox");
        segment(a, b);

        assertEquals("the quick brown fox", b.read());
        assertEquals(0, b.noiseCount());
        assertEquals(0, b.rxErrorCount());
    }

    @Test
    public void whatArrivesInATickIsHeldUntilTheTickIsOver() {
        a.write("hi");
        final List<byte[]> frames = new ArrayList<>();
        collect(frames, a);
        frames.forEach(b::receive);

        assertEquals("", drain(b.port), "a later burst in the same tick may still damage it");

        tick++;
        assertEquals("hi", b.read());
    }

    @Test
    public void lineRateCapsHowMuchATickCarries() {
        a.write("x".repeat(100));
        segment(a, b);

        assertEquals(48, b.read().length());

        segment(a, b);
        assertEquals(48, b.read().length(), "the rest follows at the same rate");
    }

    @Test
    public void slowLineStillGetsAByteOut() {
        final Endpoint slowA = new Endpoint(1, B300);
        final Endpoint slowB = new Endpoint(2, B300);

        slowA.write("abcd");

        final StringBuilder received = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            segment(slowA, slowB);
            received.append(slowB.read());
        }

        assertEquals("abcd", received.toString(), "300 baud is under a byte per tick, but not zero");
    }

    @Test
    public void fullBurstAtALowRateArrivesClean() {
        final Endpoint slowA = new Endpoint(1, B300);
        final Endpoint slowB = new Endpoint(2, B300);

        slowA.write("ab");
        segment(slowA, slowB);

        assertEquals("ab", slowB.read());
        assertEquals(0, slowB.rxErrorCount(), "what one sender can put on the line in a tick is not an overload");
    }

    @Test
    public void endpointStrappedToAnotherRateHearsNoise() {
        final Endpoint fast = new Endpoint(2, B19200);

        a.write("hello");
        segment(a, fast);

        assertEquals(1, fast.noiseCount());
        assertTrue(fast.hasFramingError(), "a rate mismatch is line noise, not a clean read");
    }

    @Test
    public void mismatchedRateArrivesAsGarbageOfTheLengthThisPortSamples() {
        final Endpoint slow = new Endpoint(2, B1200);

        a.write("x".repeat(48));
        segment(a, slow);

        final String heard = slow.read();
        assertEquals(6, heard.length(), "1200 baud samples an eighth of what 9600 baud sent");
        assertNotEquals("xxxxxx", heard, "and what it samples is not the data");
        assertTrue(slow.hasFramingError());
    }

    @Test
    public void fasterPeerAtTheWrongRateIsNoiseNotAnOverload() {
        final Endpoint fast = new Endpoint(3, B19200);

        fast.write("x".repeat(96));
        segment(fast, b);

        assertEquals(1, b.noiseCount());
        assertEquals(0, b.rxErrorCount(), "the burst is too much for 9600 baud, but the rate is what is wrong");
    }

    @Test
    public void guestProgrammingTheWrongDivisorHearsNoise() {
        b.setBaudRate(B1200);

        a.write("hello");
        segment(a, b);

        assertEquals(1, b.noiseCount());
        assertTrue(b.hasFramingError());
    }

    @Test
    public void talkingOverEachOtherDamagesEverythingInTheTick() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("x".repeat(48));
        c.write("y".repeat(48));

        final List<byte[]> frames = new ArrayList<>();
        collect(frames, a);
        collect(frames, c);
        frames.forEach(b::receive);
        tick++;

        assertEquals(1, b.rxErrorCount(), "one damaged tick, not one damaged burst");
        final String heard = b.read();
        assertEquals(48, heard.length(), "the listener samples one tick of line");
        assertFalse(heard.contains("xxxx"), "the burst that arrived first is not spared");
        assertTrue(b.hasFramingError());
    }

    @Test
    public void everyEndpointAgreesOnACollision() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("x".repeat(48));
        c.write("y".repeat(48));
        segment(a, b, c);

        assertEquals(1, a.txErrorCount(), "a sender should find out it was stepped on");
        assertEquals(1, c.txErrorCount(), "and so should the other one");
        assertEquals(0, b.txErrorCount(), "the interface that only listened sent nothing");
        assertEquals(1, b.rxErrorCount(), "what the listener saw is the other side of it");
        assertTrue(a.hasFramingError(), "the senders hear the damage too, no one gets a clean copy");
        assertTrue(c.hasFramingError());
    }

    @Test
    public void everyEndpointAgreesOnWhereTheLineLimitIs() {
        assertTheLineLimitIsShared(B9600, 48);
        assertTheLineLimitIsShared(B115200, 576);
    }

    @Test
    public void receivingBeforeSendingInATickIsStillACollision() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("x".repeat(40));
        b.write("y".repeat(40));

        final List<byte[]> fromA = new ArrayList<>();
        collect(fromA, a);
        fromA.forEach(b::receive);

        final List<byte[]> fromB = new ArrayList<>();
        collect(fromB, b);
        assertEquals(1, fromB.size(), "b still sends in the tick it already heard a in");

        fromB.forEach(a::receive);
        fromA.forEach(c::receive);
        fromB.forEach(c::receive);
        tick++;

        assertEquals(1, a.txErrorCount());
        assertEquals(1, b.txErrorCount());
        assertEquals(1, c.rxErrorCount());
    }

    @Test
    public void rateChangeAfterSendingDoesNotChangeTheVerdict() {
        a.write("hello");
        b.write("world");

        final List<byte[]> fromA = new ArrayList<>();
        collect(fromA, a);
        final List<byte[]> fromB = new ArrayList<>();
        collect(fromB, b);

        a.setBaudRate(B19200);

        fromB.forEach(a::receive);
        fromA.forEach(b::receive);
        tick++;

        assertEquals(0, a.noiseCount(), "the tick is judged at the rate a sent at");
        assertEquals(0, a.txErrorCount());
        assertEquals("world", a.read());
        assertEquals(0, b.txErrorCount());
    }

    @Test
    public void rateChangeBeforeTheTickIsOverDoesNotChangeTheVerdict() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("x".repeat(48));
        c.write("y".repeat(10));

        final List<byte[]> frames = new ArrayList<>();
        collect(frames, a);
        collect(frames, c);
        frames.forEach(b::receive);

        b.setBaudRate(B115200);
        tick++;

        assertEquals(1, b.rxErrorCount(), "58 bytes overloaded a 9600 baud tick, whatever the rate is now");
    }

    @Test
    public void whatArrivedBeforeAGapIsStillDelivered() {
        a.write("hi");
        final List<byte[]> frames = new ArrayList<>();
        collect(frames, a);
        frames.forEach(b::receive);

        tick += 5;

        assertEquals("hi", b.read());
    }

    @Test
    public void talkingWhileTrafficAtAnotherRateArrivesIsACollision() {
        final Endpoint fast = new Endpoint(3, B19200);

        a.write("ping");
        fast.write("pong");
        segment(a, b, fast);

        assertEquals(1, a.txErrorCount(), "the line was disturbed while a talked, however little it was");
        assertEquals(1, fast.txErrorCount());
        assertEquals(1, a.noiseCount());
        assertEquals(1, b.noiseCount());
        assertTrue(b.hasFramingError(), "the listener at a's rate does not get a's burst clean either");
    }

    @Test
    public void talkingAloneIsNotACollision() {
        a.write("x".repeat(48));
        segment(a, b);
        segment(a, b);

        assertEquals(0, a.txErrorCount(), "nobody else was talking");

        tick++;
        a.write("y".repeat(96));
        segment(a, b);
        segment(a, b);

        assertEquals(0, a.txErrorCount());
    }

    @Test
    public void shortBurstsThatFitDoNotCollide() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("ping");
        c.write("pong");
        segment(a, b, c);

        assertEquals(0, a.txErrorCount(), "the line had room for both");
        assertEquals(0, c.txErrorCount());
        assertEquals(0, b.rxErrorCount());
        assertEquals("pong", a.read());
        assertEquals("pingpong", b.read());
    }

    @Test
    public void takingTurnsKeepsTheSegmentClean() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("x".repeat(48));
        segment(a, b, c);
        assertEquals(0, b.rxErrorCount());

        c.write("y".repeat(48));
        segment(a, b, c);

        assertEquals(0, b.rxErrorCount(), "one sender per tick fits");
        assertEquals(0, a.txErrorCount());
        assertEquals(0, c.txErrorCount());
    }

    @Test
    public void guestThatReadsEveryTickLosesNothing() {
        for (int i = 0; i < 4; i++) {
            a.write("x".repeat(48));
            segment(a, b);
            assertEquals(48, b.read().length());
        }

        assertEquals(0, b.overrunCount());
    }

    @Test
    public void guestThatDoesNotReadForTwoTicksLosesBytes() {
        for (int i = 0; i < 4; i++) {
            a.write("x".repeat(48));
            segment(a, b);
        }

        assertEquals(80, b.overrunCount());
        assertEquals("x".repeat(112), b.read(), "what was kept is the oldest data, not a mix");
    }

    @Test
    public void whatTheCountersSawSurvivesASave() {
        final Endpoint c = new Endpoint(3, B9600);

        a.write("x".repeat(48));
        c.write("y".repeat(48));
        segment(a, b, c);

        assertEquals(1, a.txErrorCount());
        assertEquals(1, b.rxErrorCount());

        assertEquals(1, roundTrip(a.line, new BufferedSerialDevice()).getTxErrorCount());
        assertEquals(1, roundTrip(b.line, new BufferedSerialDevice()).getRxErrorCount());
    }

    @Test
    public void whatArrivedInTheLastTickSurvivesASave() {
        a.write("hi");
        final List<byte[]> frames = new ArrayList<>();
        collect(frames, a);
        frames.forEach(b::receive);

        final BufferedSerialDevice port = new BufferedSerialDevice();
        final SerialLine restored = roundTrip(b.line, port);
        tick++;
        restored.currentTick();

        assertEquals("hi", drain(port), "the burst is delivered once the tick is over");
    }

    @Test
    public void nothingIsSentWithoutTraffic() {
        assertNull(a.line.frameForTick());
    }

    @Test
    public void foreignTrafficIsIgnored() {
        b.line.writeEthernetFrame(new byte[64]);
        tick++;

        assertEquals("", b.read());
        assertEquals(0, b.noiseCount());
    }

    // --------------------------------------------------------------------- //

    private void assertTheLineLimitIsShared(final int baudRate, final int limit) {
        final Endpoint sender = new Endpoint(1, baudRate);
        final Endpoint other = new Endpoint(2, baudRate);
        final Endpoint listener = new Endpoint(3, baudRate);

        sender.write("x".repeat(limit - 1));
        other.write("y");
        segment(sender, other, listener);

        assertEquals(0, sender.txErrorCount(), "exactly one tick of line is not a collision");
        assertEquals(0, other.txErrorCount());
        assertEquals(0, listener.rxErrorCount());
        assertEquals(limit, listener.read().length());

        tick++;
        sender.write("x".repeat(limit - 1));
        other.write("yy");
        segment(sender, other, listener);

        assertEquals(1, sender.txErrorCount(), "one byte more is");
        assertEquals(1, other.txErrorCount());
        assertEquals(1, sender.rxErrorCount());
        assertEquals(1, other.rxErrorCount());
        assertEquals(1, listener.rxErrorCount());
    }

    private SerialLine roundTrip(final SerialLine line, final BufferedSerialDevice port) {
        final SerialLine restored = new SerialLine(port, () -> tick, new byte[]{0x02, 0x6F, 0x63, 0, 0, 9});
        return NBTSerialization.deserialize(NBTSerialization.serialize(line), restored);
    }

    private void segment(final Endpoint... endpoints) {
        final List<byte[]> frames = new ArrayList<>();
        for (final Endpoint endpoint : endpoints) {
            collect(frames, endpoint);
        }
        for (final byte[] frame : frames) {
            for (final Endpoint endpoint : endpoints) {
                endpoint.receive(frame);
            }
        }
        tick++;
    }

    private static void collect(final List<byte[]> frames, final Endpoint endpoint) {
        endpoint.port.step(0);
        final byte[] frame = endpoint.line.frameForTick();
        if (frame != null) {
            frames.add(frame);
        }
    }

    private static String drain(final BufferedSerialDevice port) {
        final StringBuilder result = new StringBuilder();
        port.step(0);
        while ((lineStatus(port) & UART_LSR_DR) != 0) {
            result.append((char) (port.load(UART_RBR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF));
            port.step(0);
        }
        return result.toString();
    }

    private static int lineStatus(final BufferedSerialDevice port) {
        return (int) port.load(UART_LSR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF;
    }

    // --------------------------------------------------------------------- //

    private final class Endpoint {
        private final BufferedSerialDevice port = new BufferedSerialDevice();
        private final SerialLine line;
        private boolean framingErrorSeen;

        private Endpoint(final int id, final int baudRate) {
            port.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);
            setBaudRate(baudRate);

            final byte[] mac = {0x02, 0x6F, 0x63, 0, 0, (byte) id};
            line = new SerialLine(port, () -> tick, mac);
        }

        private void setBaudRate(final int baudRate) {
            final int divisor = UART16550A.CLOCK_FREQUENCY / (16 * baudRate);
            port.store(UART_LCR_OFFSET, UART_LCR_DLAB, Sizes.SIZE_8_LOG2);
            port.store(UART_DLL_OFFSET, divisor & 0xFF, Sizes.SIZE_8_LOG2);
            port.store(UART_DLM_OFFSET, divisor >>> 8, Sizes.SIZE_8_LOG2);
            port.store(UART_LCR_OFFSET, 0, Sizes.SIZE_8_LOG2);
        }

        private void write(final String value) {
            for (final byte b : value.getBytes(StandardCharsets.UTF_8)) {
                port.store(UART_THR_OFFSET, b, Sizes.SIZE_8_LOG2);
                port.step(0);
            }
        }

        private String read() {
            line.currentTick();
            final StringBuilder result = new StringBuilder();
            port.step(0);
            int status = lineStatus(port);
            while ((status & UART_LSR_DR) != 0) {
                framingErrorSeen |= (status & UART_LSR_FE) != 0;
                result.append((char) (port.load(UART_RBR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF));
                port.step(0);
                status = lineStatus(port);
            }
            framingErrorSeen |= (status & UART_LSR_FE) != 0;
            return result.toString();
        }

        private boolean hasFramingError() {
            line.currentTick();
            port.step(0);
            framingErrorSeen |= (lineStatus(port) & UART_LSR_FE) != 0;
            return framingErrorSeen;
        }

        private int overrunCount() {
            line.currentTick();
            return line.getOverrunCount();
        }

        private int rxErrorCount() {
            line.currentTick();
            return line.getRxErrorCount();
        }

        private int txErrorCount() {
            line.currentTick();
            return line.getTxErrorCount();
        }

        private int noiseCount() {
            line.currentTick();
            return line.getNoiseCount();
        }

        private void receive(@Nullable final byte[] frame) {
            if (frame != null) {
                line.writeEthernetFrame(frame);
            }
        }
    }
}

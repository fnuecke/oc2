/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.sedna.Sedna;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

public final class SerialPortTests {
    private static final int TICKS_TO_ARRIVE = 3;

    private long tick;
    private SerialPort a;
    private SerialPort b;

    @BeforeAll
    public static void setUpAll() {
        Sedna.initialize();
    }

    @BeforeEach
    public void setUp() {
        tick = 1;
        a = new SerialPort(() -> tick);
        b = new SerialPort(() -> tick);
        a.setConfiguration(1, SerialEndpoint.DEFAULT_BAUD_RATE);
        b.setConfiguration(2, SerialEndpoint.DEFAULT_BAUD_RATE);
    }

    // --------------------------------------------------------------------- //

    @Test
    public void writtenBytesArriveAtThePeer() {
        assertEquals(4, a.write(bytes("ping")));
        ticks(TICKS_TO_ARRIVE);

        assertEquals(4, b.available());
        assertEquals("ping", string(b.read(Integer.MAX_VALUE)));
        assertEquals(0, b.available());
        assertEquals(0, a.available(), "a port does not hear itself");
    }

    @Test
    public void slowReaderKeepsMoreThanTheLineBacklog() {
        final byte[] data = bytes("x".repeat(400));
        int written = 0;
        for (int i = 0; i < 20; i++) {
            written += a.write(Arrays.copyOfRange(data, written, data.length));
            ticks(1);
        }
        ticks(TICKS_TO_ARRIVE);

        assertEquals(400, written);
        assertEquals(400, b.available(), "what the reader has not fetched yet is not lost to the line's two tick backlog");
        assertEquals(0, b.getOverrunCount());
    }

    @Test
    public void peekLeavesTheValueAndSkipDropsIt() {
        a.write(bytes("ab"));
        ticks(TICKS_TO_ARRIVE);

        assertEquals('a', b.peek());
        assertEquals('a', b.peek());
        b.skip();
        assertEquals('b', b.peek());
        b.skip();
        assertEquals(-1, b.peek());
        b.skip();
    }

    @Test
    public void mismatchedRateArrivesFlagged() {
        b.setConfiguration(2, 19200);

        a.write(bytes("hello"));
        ticks(TICKS_TO_ARRIVE);

        final short[] values = b.read(Integer.MAX_VALUE);
        assertTrue(values.length > 0);
        for (final short value : values) {
            assertNotEquals(0, value & BufferedSerialDevice.FRAME_ERROR_FLAG);
        }
        assertEquals(1, b.getNoiseCount());
    }

    @Test
    public void listenerRunsOnceWhenDataArrives() {
        final int[] notifications = {0};
        b.setListener(() -> notifications[0]++);

        a.write(bytes("hi"));
        ticks(TICKS_TO_ARRIVE + 2);

        assertEquals(1, notifications[0]);
        assertEquals(2, b.available(), "the listener announces data, it does not consume it");
    }

    @Test
    public void clearDropsWhatWasReceived() {
        a.write(bytes("ab"));
        ticks(TICKS_TO_ARRIVE);

        b.clear();
        assertEquals(0, b.available());
    }

    // --------------------------------------------------------------------- //

    private void ticks(final int count) {
        for (int i = 0; i < count; i++) {
            exchange(a, b);
            exchange(b, a);
            tick++;
        }
    }

    private static void exchange(final SerialPort from, final SerialPort to) {
        final byte[] frame = from.readEthernetFrame();
        if (frame != null) {
            to.writeEthernetFrame(from, frame, 1);
        }
    }

    private static byte[] bytes(final String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static String string(final short[] values) {
        final byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}

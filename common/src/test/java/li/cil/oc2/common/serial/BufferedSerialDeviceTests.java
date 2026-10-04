/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.sedna.Sedna;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public final class BufferedSerialDeviceTests {
    @BeforeAll
    public static void setUpAll() {
        Sedna.initialize();
        Serializers.initialize();
    }

    // --------------------------------------------------------------------- //

    @Test
    public void bytesOnTheirWayToTheGuestSurviveASave() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        Uart.enableFifo(port);

        final byte[] data = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        assertEquals(data.length, port.offer(data, false));

        final BufferedSerialDevice restored = roundTrip(port);
        Uart.enableFifo(restored);

        final StringBuilder received = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            restored.step(0);
            assertNotEquals(0, Uart.lineStatus(restored) & Uart.LSR_DR, "a byte should still be waiting");
            received.append(Uart.read(restored));
        }

        assertEquals(new String(data, StandardCharsets.UTF_8), received.toString());
    }

    @Test
    public void bytesOnTheirWayToTheLineSurviveASave() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        Uart.enableFifo(port);

        for (final byte value : "hello".getBytes(StandardCharsets.UTF_8)) {
            Uart.write(port, value);
            port.step(0);
        }

        final byte[] pending = roundTrip(port).poll(16);

        assertNotNull(pending, "what the guest wrote should still be there to send");
        assertEquals("hello", new String(pending, StandardCharsets.UTF_8));
    }

    @Test
    public void disturbedByteIsStillDisturbedAfterASave() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        Uart.enableFifo(port);
        port.offer(new byte[]{'x'}, true);

        final BufferedSerialDevice restored = roundTrip(port);
        Uart.enableFifo(restored);
        restored.step(0);

        assertNotEquals(0, Uart.lineStatus(restored) & Uart.LSR_FE, "the guest still must not trust it");
        assertEquals('x', Uart.read(restored));
    }

    @Test
    public void sendBacklogFollowsTheLineRate() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        Uart.enableFifo(port);
        Uart.setBaudRate(port, 300);

        for (int i = 0; i < 64; i++) {
            Uart.write(port, 'x');
            port.step(0);
        }

        final byte[] pending = port.poll(SerialFrame.MAX_DATA_SIZE);
        assertNotNull(pending);
        assertEquals(3, pending.length, "300 baud carries 1.5 bytes a tick, so two ticks of backlog is three bytes");
    }

    // --------------------------------------------------------------------- //

    private static BufferedSerialDevice roundTrip(final BufferedSerialDevice port) {
        return NBTSerialization.deserialize(NBTSerialization.serialize(port), new BufferedSerialDevice());
    }
}

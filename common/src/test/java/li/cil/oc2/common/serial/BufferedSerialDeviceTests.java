/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.serialization.ceres.Serializers;
import li.cil.sedna.Sedna;
import li.cil.sedna.api.Sizes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public final class BufferedSerialDeviceTests {
    private static final int UART_RBR_OFFSET = 0;
    private static final int UART_THR_OFFSET = 0;
    private static final int UART_FCR_OFFSET = 2;
    private static final int UART_LSR_OFFSET = 5;
    private static final int UART_LCR_OFFSET = 3;
    private static final int UART_DLL_OFFSET = 0;
    private static final int UART_DLM_OFFSET = 1;

    private static final int UART_FCR_FE = 1 << 0;
    private static final int UART_LCR_DLAB = 1 << 7;
    private static final int DIVISOR_300 = 384;
    private static final int UART_LSR_DR = 1 << 0;
    private static final int UART_LSR_FE = 1 << 3;

    @BeforeAll
    public static void setUpAll() {
        Sedna.initialize();
        Serializers.initialize();
    }

    // --------------------------------------------------------------------- //

    @Test
    public void bytesOnTheirWayToTheGuestSurviveASave() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        port.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);

        final byte[] data = "the quick brown fox jumps over the lazy dog".getBytes(StandardCharsets.UTF_8);
        assertEquals(data.length, port.offer(data, false));

        final BufferedSerialDevice restored = roundTrip(port);
        restored.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);

        final StringBuilder received = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            restored.step(0);
            assertNotEquals(0, lineStatus(restored) & UART_LSR_DR, "a byte should still be waiting");
            received.append((char) (restored.load(UART_RBR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF));
        }

        assertEquals(new String(data, StandardCharsets.UTF_8), received.toString());
    }

    @Test
    public void bytesOnTheirWayToTheLineSurviveASave() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        port.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);

        for (final byte value : "hello".getBytes(StandardCharsets.UTF_8)) {
            port.store(UART_THR_OFFSET, value, Sizes.SIZE_8_LOG2);
            port.step(0);
        }

        final byte[] pending = roundTrip(port).poll(16);

        assertNotNull(pending, "what the guest wrote should still be there to send");
        assertEquals("hello", new String(pending, StandardCharsets.UTF_8));
    }

    @Test
    public void disturbedByteIsStillDisturbedAfterASave() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        port.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);
        port.offer(new byte[]{'x'}, true);

        final BufferedSerialDevice restored = roundTrip(port);
        restored.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);
        restored.step(0);

        assertNotEquals(0, lineStatus(restored) & UART_LSR_FE, "the guest still must not trust it");
        assertEquals('x', restored.load(UART_RBR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF);
    }

    @Test
    public void sendBacklogFollowsTheLineRate() {
        final BufferedSerialDevice port = new BufferedSerialDevice();
        port.store(UART_FCR_OFFSET, UART_FCR_FE, Sizes.SIZE_8_LOG2);
        port.store(UART_LCR_OFFSET, UART_LCR_DLAB, Sizes.SIZE_8_LOG2);
        port.store(UART_DLL_OFFSET, DIVISOR_300 & 0xFF, Sizes.SIZE_8_LOG2);
        port.store(UART_DLM_OFFSET, DIVISOR_300 >>> 8, Sizes.SIZE_8_LOG2);
        port.store(UART_LCR_OFFSET, 0, Sizes.SIZE_8_LOG2);

        for (int i = 0; i < 64; i++) {
            port.store(UART_THR_OFFSET, 'x', Sizes.SIZE_8_LOG2);
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

    private static int lineStatus(final BufferedSerialDevice port) {
        return (int) port.load(UART_LSR_OFFSET, Sizes.SIZE_8_LOG2) & 0xFF;
    }
}

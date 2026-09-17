/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public final class SerialFrameTests {
    private static final byte[] MAC = {0x02, 0x6F, 0x63, 0x11, 0x22, 0x33};

    private static final int DIVISOR_300 = 384;
    private static final int DIVISOR_9600 = 12;
    private static final int DIVISOR_19200 = 6;
    private static final int DIVISOR_115200 = 1;

    @Test
    public void framesRoundTrip() {
        final byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        final SerialFrame frame = new SerialFrame(MAC, DIVISOR_19200, data);

        final SerialFrame decoded = SerialFrame.fromEthernetFrame(frame.toEthernetFrame());

        assertNotNull(decoded);
        assertArrayEquals(MAC, decoded.sourceMac());
        assertEquals(DIVISOR_19200, decoded.baudDivisor());
        assertArrayEquals(data, decoded.data());
    }

    @Test
    public void framesCarryHighBytes() {
        final byte[] data = {0x00, (byte) 0x80, (byte) 0xFF, 0x1A};
        final SerialFrame decoded = SerialFrame.fromEthernetFrame(
            new SerialFrame(MAC, DIVISOR_300, data).toEthernetFrame());

        assertNotNull(decoded);
        assertArrayEquals(data, decoded.data());
    }

    @Test
    public void aFullFrameRoundTrips() {
        final byte[] data = new byte[SerialFrame.MAX_DATA_SIZE];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }

        final SerialFrame decoded = SerialFrame.fromEthernetFrame(
            new SerialFrame(MAC, DIVISOR_115200, data).toEthernetFrame());

        assertNotNull(decoded);
        assertArrayEquals(data, decoded.data(), "the length field has to hold more than a byte");
    }

    @Test
    public void otherTrafficIsNotOurs() {
        final byte[] frame = new SerialFrame(MAC, DIVISOR_9600, new byte[]{1}).toEthernetFrame();

        final byte[] foreignEtherType = frame.clone();
        foreignEtherType[12] = 0x08;
        foreignEtherType[13] = 0x00;
        assertNull(SerialFrame.fromEthernetFrame(foreignEtherType), "an IPv4 frame is not ours");

        final byte[] foreignMagic = frame.clone();
        foreignMagic[14] = 'X';
        assertNull(SerialFrame.fromEthernetFrame(foreignMagic));

        final byte[] futureVersion = frame.clone();
        futureVersion[16] = 2;
        assertNull(SerialFrame.fromEthernetFrame(futureVersion));

        assertNull(SerialFrame.fromEthernetFrame(new byte[8]), "a runt is not ours either");
    }

    @Test
    public void truncatedFramesAreRejected() {
        final byte[] frame = new SerialFrame(MAC, DIVISOR_9600, new byte[]{1, 2, 3, 4}).toEthernetFrame();
        final byte[] truncated = new byte[frame.length - 2];
        System.arraycopy(frame, 0, truncated, 0, truncated.length);

        assertNull(SerialFrame.fromEthernetFrame(truncated));
    }
}

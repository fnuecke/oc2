/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.serial;

import javax.annotation.Nullable;

// Serial over ethernet, because it's easy to do over what we already have. Might want splitting out, we'll see.
// Uses the experimental ethertype for local use: https://www.iana.org/assignments/ieee-802-numbers
public record SerialFrame(byte[] sourceMac, int baudDivisor, byte[] data) {
    public static final int ETHER_TYPE = 0x88B5; // IEEE Std 802 - Local Experimental Ethertype
    public static final int MAX_DATA_SIZE = 1024;
    public static final int MAC_SIZE = 6;

    private static final int ETHERNET_HEADER_SIZE = 14;
    private static final int MAGIC = 0x4F43; // "OC"
    private static final int VERSION = 1;
    private static final int OFFSET_MAGIC = ETHERNET_HEADER_SIZE;
    private static final int OFFSET_VERSION = OFFSET_MAGIC + 2;
    private static final int OFFSET_BAUD = OFFSET_VERSION + 1;
    private static final int OFFSET_LENGTH = OFFSET_BAUD + 2;
    private static final int OFFSET_DATA = OFFSET_LENGTH + 2;

    // --------------------------------------------------------------------- //

    public SerialFrame {
        if (sourceMac.length != MAC_SIZE) {
            throw new IllegalArgumentException("Source address is not a MAC address.");
        }
        if (data.length == 0 || data.length > MAX_DATA_SIZE) {
            throw new IllegalArgumentException("Frame carries no data or more than a frame holds.");
        }
    }

    // --------------------------------------------------------------------- //

    public byte[] toEthernetFrame() {
        final byte[] frame = new byte[OFFSET_DATA + data.length];

        for (int i = 0; i < MAC_SIZE; i++) {
            frame[i] = (byte) 0xFF; // Broadcast; the segment is the addressing unit.
            frame[MAC_SIZE + i] = sourceMac[i];
        }

        frame[12] = (byte) (ETHER_TYPE >>> 8);
        frame[13] = (byte) ETHER_TYPE;
        frame[OFFSET_MAGIC] = (byte) (MAGIC >>> 8);
        frame[OFFSET_MAGIC + 1] = (byte) MAGIC;
        frame[OFFSET_VERSION] = VERSION;
        frame[OFFSET_BAUD] = (byte) (baudDivisor >>> 8);
        frame[OFFSET_BAUD + 1] = (byte) baudDivisor;
        frame[OFFSET_LENGTH] = (byte) (data.length >>> 8);
        frame[OFFSET_LENGTH + 1] = (byte) data.length;

        System.arraycopy(data, 0, frame, OFFSET_DATA, data.length);

        return frame;
    }

    @Nullable
    public static SerialFrame fromEthernetFrame(final byte[] frame) {
        if (frame.length < OFFSET_DATA) {
            return null;
        }
        if (((frame[12] & 0xFF) << 8 | (frame[13] & 0xFF)) != ETHER_TYPE) {
            return null;
        }
        if (((frame[OFFSET_MAGIC] & 0xFF) << 8 | (frame[OFFSET_MAGIC + 1] & 0xFF)) != MAGIC) {
            return null;
        }
        if ((frame[OFFSET_VERSION] & 0xFF) != VERSION) {
            return null;
        }

        final int length = (frame[OFFSET_LENGTH] & 0xFF) << 8 | (frame[OFFSET_LENGTH + 1] & 0xFF);
        if (length == 0 || length > MAX_DATA_SIZE || frame.length < OFFSET_DATA + length) {
            return null;
        }

        final byte[] sourceMac = new byte[MAC_SIZE];
        System.arraycopy(frame, MAC_SIZE, sourceMac, 0, MAC_SIZE);

        final byte[] data = new byte[length];
        System.arraycopy(frame, OFFSET_DATA, data, 0, length);

        return new SerialFrame(sourceMac, (frame[OFFSET_BAUD] & 0xFF) << 8 | (frame[OFFSET_BAUD + 1] & 0xFF), data);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;

public final class TcpHeader {
    public static final int SIZE_WITHOUT_PORTS = 16;
    public static final int MIN_HEADER_SIZE = 20;
    public static final int MAX_HEADER_SIZE = 60;

    public static final int FLAG_FIN = 1;
    public static final int FLAG_SYN = 1 << 1;
    public static final int FLAG_RST = 1 << 2;
    public static final int FLAG_PSH = 1 << 3;
    public static final int FLAG_ACK = 1 << 4;
    public static final int FLAG_URG = 1 << 5;

    private static final int INVALID_OPTIONS = Integer.MIN_VALUE;
    private static final byte OPTION_END = 0;
    private static final byte OPTION_NOOP = 1;
    private static final byte OPTION_MAX_SEGMENT_SIZE = 2;
    public static final int CHECKSUM_OFFSET = 16;

    // --------------------------------------------------------------------- //

    public int sequenceNumber;
    public int acknowledgmentNumber;
    public boolean urg, ack, psh, rst, syn, fin;
    public int window;
    public int urgentPointer;
    public int maxSegmentSize;

    // --------------------------------------------------------------------- //

    public boolean read(final ByteBuffer data) {
        if (data.remaining() < SIZE_WITHOUT_PORTS) {
            return false;
        }

        final int start = data.position();
        sequenceNumber = data.getInt();
        acknowledgmentNumber = data.getInt();

        final int dataOffsetWords = Byte.toUnsignedInt(data.get()) >>> 4;
        final int headerSize = dataOffsetWords * 4;
        if (headerSize < MIN_HEADER_SIZE || headerSize > MAX_HEADER_SIZE) {
            return false;
        }
        // start is already past the ports, so the options end that many bytes further in.
        final int optionsEnd = start + headerSize - 4;
        if (optionsEnd > data.limit()) {
            return false;
        }

        final int flags = Byte.toUnsignedInt(data.get());
        urg = (flags & FLAG_URG) != 0;
        ack = (flags & FLAG_ACK) != 0;
        psh = (flags & FLAG_PSH) != 0;
        rst = (flags & FLAG_RST) != 0;
        syn = (flags & FLAG_SYN) != 0;
        fin = (flags & FLAG_FIN) != 0;

        window = Short.toUnsignedInt(data.getShort());
        data.getShort(); // Checksum; the link to the guest cannot corrupt anything.
        urgentPointer = Short.toUnsignedInt(data.getShort());

        maxSegmentSize = readOptions(data, optionsEnd);
        if (maxSegmentSize == INVALID_OPTIONS) {
            return false;
        }

        data.position(optionsEnd);
        return true;
    }

    public void write(final ByteBuffer data) {
        data.putInt(sequenceNumber);
        data.putInt(acknowledgmentNumber);

        final int headerSize = headerSize();
        data.put((byte) ((headerSize / 4) << 4));

        int flags = 0;
        if (urg) flags |= FLAG_URG;
        if (ack) flags |= FLAG_ACK;
        if (psh) flags |= FLAG_PSH;
        if (rst) flags |= FLAG_RST;
        if (syn) flags |= FLAG_SYN;
        if (fin) flags |= FLAG_FIN;
        data.put((byte) flags);

        data.putShort((short) window);
        data.putShort((short) 0); // Checksum, filled in once the payload is in place.
        data.putShort((short) urgentPointer);

        if (maxSegmentSize != -1) {
            data.put(OPTION_MAX_SEGMENT_SIZE);
            data.put((byte) 4);
            data.putShort((short) maxSegmentSize);
        }
    }

    public int headerSize() {
        return MIN_HEADER_SIZE + (maxSegmentSize == -1 ? 0 : 4);
    }

    public void clear() {
        sequenceNumber = 0;
        acknowledgmentNumber = 0;
        urg = ack = psh = rst = syn = fin = false;
        window = 0;
        urgentPointer = 0;
        maxSegmentSize = -1;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("TCP[seq=");
        builder.append(Integer.toUnsignedString(sequenceNumber));
        builder.append(" ack=").append(Integer.toUnsignedString(acknowledgmentNumber));
        builder.append(" win=").append(window).append(' ');
        if (urg) builder.append('U');
        if (ack) builder.append('A');
        if (psh) builder.append('P');
        if (rst) builder.append('R');
        if (syn) builder.append('S');
        if (fin) builder.append('F');
        if (maxSegmentSize != -1) {
            builder.append(" mss=").append(maxSegmentSize);
        }
        return builder.append(']').toString();
    }

    // --------------------------------------------------------------------- //

    private int readOptions(final ByteBuffer data, final int optionsEnd) {
        int advertisedMaxSegmentSize = -1;
        while (data.position() < optionsEnd) {
            final byte kind = data.get();
            if (kind == OPTION_END) {
                return advertisedMaxSegmentSize;
            }
            if (kind == OPTION_NOOP) {
                continue;
            }
            if (data.position() >= optionsEnd) {
                return INVALID_OPTIONS;
            }
            final int length = Byte.toUnsignedInt(data.get());
            if (length < 2 || data.position() + length - 2 > optionsEnd) {
                return INVALID_OPTIONS;
            }
            if (kind == OPTION_MAX_SEGMENT_SIZE) {
                if (length != 4) {
                    return INVALID_OPTIONS;
                }
                advertisedMaxSegmentSize = Short.toUnsignedInt(data.getShort());
            } else {
                data.position(data.position() + length - 2);
            }
        }
        return advertisedMaxSegmentSize;
    }
}

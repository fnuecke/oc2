package li.cil.oc2.common.inet;

import java.nio.ByteBuffer;

public class TcpHeader {
    public static final int MIN_HEADER_SIZE_NO_PORTS = 16;

    private static final byte OPTION_END = 0;
    private static final byte OPTION_NOOP = 1;
    private static final byte OPTION_MAX_SEGMENT_SIZE = 2;

    ////////////////////////////////////////////////////////////////////////////

    public int sequenceNumber, acknowledgmentNumber;
    public boolean urg, ack, psh, rst, syn, fin; // flags
    public int window;
    public int urgentPointer;

    // Options
    public int maxSegmentSize;

    ////////////////////////////////////////////////////////////////////////////

    public boolean read(final ByteBuffer data) {
        if (data.remaining() < MIN_HEADER_SIZE_NO_PORTS) {
            System.out.println("A");
            return false;
        }
        final int position = data.position();
        sequenceNumber = data.getInt();
        acknowledgmentNumber = data.getInt();
        final int dataOffset = position + ((data.get() >>> 2) & 0x3C) - 4;
        if (dataOffset > data.limit()) {
            System.out.println("C dataOffset=" + dataOffset + ", data.limit()=" + data.limit());
            return false;
        }
        final int flags = Byte.toUnsignedInt(data.get());
        urg = ((flags >>> 5) & 1) == 1;
        ack = ((flags >>> 4) & 1) == 1;
        psh = ((flags >>> 3) & 1) == 1;
        rst = ((flags >>> 2) & 1) == 1;
        syn = ((flags >>> 1) & 1) == 1;
        fin = (flags & 1) == 1;
        window = Short.toUnsignedInt(data.getShort());
        data.getShort(); // checksum
        urgentPointer = Short.toUnsignedInt(data.getShort());

        maxSegmentSize = -1;

        while (dataOffset > data.position()) {
            final byte type = data.get();
            switch (type) {
                case OPTION_END:
                    data.position(dataOffset);
                    return true;
                case OPTION_NOOP:
                    continue;
                default:
                    break;
            }
            final int size = Byte.toUnsignedInt(data.get());
            if (type == OPTION_MAX_SEGMENT_SIZE) {
                if (size != 4) {
                    data.position(position);
                    System.out.println("B");
                    return false;
                }
                maxSegmentSize = Short.toUnsignedInt(data.getShort());
            } else {
                // Skip unknown option
                data.position(data.position() + size - 2);
            }
        }
        data.position(dataOffset);
        return true;
    }

    private int bool2int(boolean value) {
        return value ? 1 : 0;
    }

    public void write(final ByteBuffer data) {
        data.putInt(sequenceNumber);
        data.putInt(acknowledgmentNumber);
        final int headerLength = 4 + MIN_HEADER_SIZE_NO_PORTS + (maxSegmentSize == -1 ? 0 : 4);
        data.put((byte) (headerLength << 2));
        final int flags =
                (bool2int(urg) << 5) |
                (bool2int(ack) << 4) |
                (bool2int(psh) << 3) |
                (bool2int(rst) << 2) |
                (bool2int(syn) << 1) |
                (bool2int(fin));
        data.put((byte) flags);
        data.putShort((short) window);
        data.putShort((short) 0); // checksum
        data.putShort((short) urgentPointer);

        // Options
        if (maxSegmentSize != -1) {
            data.put(OPTION_MAX_SEGMENT_SIZE);
            data.put((byte) 4);
            data.putShort((short) maxSegmentSize);
        }
    }

    public boolean isConnectionInitiation() {
        return syn && !urg && !ack && !psh && !rst && !fin;
    }

    public void acceptConnection(final int sequence, final int acknowledgment, final int window) {
        sequenceNumber = sequence;
        acknowledgmentNumber = acknowledgment;
        urg = false;
        ack = true;
        psh = false;
        rst = false;
        syn = true;
        fin = false;
        this.window = window;
        urgentPointer = 0;
        maxSegmentSize = -1;
    }

    public boolean isAcceptanceOrRejectionAcknowledged() {
        return !syn && !urg && ack && !psh && !rst && !fin;
    }

    public void rejectConnection(final int sequence, final int acknowledgment) {
        sequenceNumber = sequence;
        acknowledgmentNumber = acknowledgment;
        urg = false;
        ack = true;
        psh = false;
        rst = true;
        syn = false;
        fin = false;
        window = 0;
        urgentPointer = 0;
        maxSegmentSize = -1;
    }

    @Override
    public String toString() {
        return "TcpHeader{" +
               "sequenceNumber=" + sequenceNumber +
               ", acknowledgmentNumber=" + acknowledgmentNumber +
               ", urg=" + urg +
               ", ack=" + ack +
               ", psh=" + psh +
               ", rst=" + rst +
               ", syn=" + syn +
               ", fin=" + fin +
               ", window=" + window +
               ", urgentPointer=" + urgentPointer +
               ", maxSegmentSize=" + maxSegmentSize +
               '}';
    }
}

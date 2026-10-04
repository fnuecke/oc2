/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Named.named;

public class TcpHeaderTests {
    private static final byte[] NO_BYTES = {};

    // --------------------------------------------------------------------- //

    @Test
    public void plainHeaderParses() {
        final TcpHeader header = new TcpHeader();
        final ByteBuffer data = segment(1000, 2000, 5, TcpHeader.FLAG_ACK | TcpHeader.FLAG_PSH,
            NO_BYTES, new byte[]{1, 2, 3});

        assertTrue(header.read(data));
        assertEquals(1000, header.sequenceNumber);
        assertEquals(2000, header.acknowledgmentNumber);
        assertTrue(header.ack);
        assertTrue(header.psh);
        assertFalse(header.syn);
        assertFalse(header.fin);
        assertFalse(header.rst);
        assertEquals(8192, header.window);
        assertEquals(-1, header.maxSegmentSize);
        assertEquals(3, data.remaining());
    }

    @Test
    public void maximumSegmentSizeOptionIsRead() {
        final TcpHeader header = new TcpHeader();
        final byte[] options = {2, 4, 0x05, (byte) 0xB4};
        final ByteBuffer data = segment(1, 0, 6, TcpHeader.FLAG_SYN, options, NO_BYTES);

        assertTrue(header.read(data));
        assertEquals(1460, header.maxSegmentSize);
        assertTrue(header.syn);
        assertEquals(0, data.remaining());
    }

    @Test
    public void unknownOptionsAreSkipped() {
        final TcpHeader header = new TcpHeader();
        final byte[] options = {1, 3, 3, 7, 1, 2, 4, 0x05, (byte) 0xB4};
        final byte[] padded = new byte[12];
        System.arraycopy(options, 0, padded, 0, options.length);
        final ByteBuffer data = segment(1, 0, 8, TcpHeader.FLAG_SYN, padded, new byte[]{9});

        assertTrue(header.read(data));
        assertEquals(1460, header.maxSegmentSize);
        assertEquals(1, data.remaining());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("malformedOptions")
    public void malformedOptionIsRejected(final byte[] options) {
        final TcpHeader header = new TcpHeader();
        final ByteBuffer data = segment(1, 0, 5 + options.length / 4, TcpHeader.FLAG_SYN, options, NO_BYTES);

        assertFalse(header.read(data));
    }

    @Test
    public void dataOffsetBelowTheMinimumIsRejected() {
        final TcpHeader header = new TcpHeader();
        final ByteBuffer data = segment(1, 0, 4, TcpHeader.FLAG_ACK, NO_BYTES, NO_BYTES);

        assertFalse(header.read(data));
    }

    @Test
    public void dataOffsetPastTheSegmentIsRejected() {
        final TcpHeader header = new TcpHeader();
        final ByteBuffer data = segment(1, 0, 15, TcpHeader.FLAG_ACK, NO_BYTES, NO_BYTES);

        assertFalse(header.read(data));
    }

    @Test
    public void truncatedHeaderIsRejected() {
        final TcpHeader header = new TcpHeader();
        final ByteBuffer data = ByteBuffer.allocate(TcpHeader.SIZE_WITHOUT_PORTS - 1);

        assertFalse(header.read(data));
    }

    @Test
    public void optionListEndMarkerStopsTheWalk() {
        final TcpHeader header = new TcpHeader();
        final byte[] options = {2, 4, 0x05, (byte) 0xB4, 0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        final ByteBuffer data = segment(1, 0, 7, TcpHeader.FLAG_SYN, options, NO_BYTES);

        assertTrue(header.read(data));
        assertEquals(1460, header.maxSegmentSize);
    }

    @Test
    public void writtenHeaderReadsBackIdentically() {
        final TcpHeader written = new TcpHeader();
        written.clear();
        written.sequenceNumber = 0x12345678;
        written.acknowledgmentNumber = 0x9ABCDEF0;
        written.ack = true;
        written.syn = true;
        written.window = 4096;
        written.maxSegmentSize = 1460;

        final ByteBuffer buffer = ByteBuffer.allocate(64);
        written.write(buffer);
        buffer.flip();

        final TcpHeader read = new TcpHeader();
        assertTrue(read.read(buffer));
        assertEquals(written.sequenceNumber, read.sequenceNumber);
        assertEquals(written.acknowledgmentNumber, read.acknowledgmentNumber);
        assertTrue(read.ack);
        assertTrue(read.syn);
        assertFalse(read.fin);
        assertEquals(4096, read.window);
        assertEquals(1460, read.maxSegmentSize);
    }

    @Test
    public void writtenHeaderReportsTheSizeItOccupies() {
        final TcpHeader header = new TcpHeader();
        header.clear();
        assertEquals(TcpHeader.MIN_HEADER_SIZE, header.headerSize());

        header.maxSegmentSize = 1460;
        assertEquals(TcpHeader.MIN_HEADER_SIZE + 4, header.headerSize());

        final ByteBuffer buffer = ByteBuffer.allocate(64);
        header.write(buffer);
        assertEquals(header.headerSize() - 4, buffer.position());
    }

    // --------------------------------------------------------------------- //

    private static Stream<Named<byte[]>> malformedOptions() {
        return Stream.of(
            named("zero length, would loop forever", new byte[]{8, 0, 0, 0}),
            named("length one", new byte[]{8, 1, 0, 0}),
            named("running past the header", new byte[]{8, (byte) 40, 0, 0}),
            named("maximum segment size with wrong length", new byte[]{2, 6, 0, 0, 0, 0, 0, 0})
        );
    }

    private static ByteBuffer segment(
        final int sequenceNumber,
        final int acknowledgmentNumber,
        final int dataOffsetWords,
        final int flags,
        final byte[] options,
        final byte[] payload
    ) {
        final ByteBuffer buffer = ByteBuffer.allocate(
            TcpHeader.SIZE_WITHOUT_PORTS + options.length + payload.length);
        buffer.putInt(sequenceNumber);
        buffer.putInt(acknowledgmentNumber);
        buffer.put((byte) (dataOffsetWords << 4));
        buffer.put((byte) flags);
        buffer.putShort((short) 8192);
        buffer.putShort((short) 0);
        buffer.putShort((short) 0);
        buffer.put(options);
        buffer.put(payload);
        buffer.flip();
        return buffer;
    }
}

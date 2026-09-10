/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("resource")
public final class IOStreamsTests {
    @Test
    public void bytesReadBackUnsigned() throws Exception {
        final IOInputStream stream = read(0x00, 0x7F, 0xFF);
        assertEquals(0, stream.readU8());
        assertEquals(127, stream.readU8());
        assertEquals(255, stream.readU8());
    }

    @Test
    public void wideValuesReadLowByteFirst() throws Exception {
        // The same bytes through DataInputStream would read as 0x3412; the guests are little-endian.
        assertEquals(0x1234, read(0x34, 0x12).readU16());
        assertEquals(0xFFFFFFFFL, read(0xFF, 0xFF, 0xFF, 0xFF).readU32());
        assertEquals(0x12345678L, read(0x78, 0x56, 0x34, 0x12).readU32());
    }

    @Test
    public void wideValuesWriteLowByteFirst() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final IOOutputStream stream = new IOOutputStream(bytes);
        stream.writeU16(0x1234);
        stream.writeU32(0x12345678L);

        assertArrayEquals(new byte[]{0x34, 0x12, 0x78, 0x56, 0x34, 0x12}, bytes.toByteArray());
    }

    @Test
    public void wideValuesRoundTrip() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final IOOutputStream out = new IOOutputStream(bytes);
        out.writeU8(0xFE);
        out.writeU16(0xBEEF);
        out.writeU32(0xDEADBEEFL);

        final IOInputStream in = new IOInputStream(new ByteArrayInputStream(bytes.toByteArray()));
        assertEquals(0xFE, in.readU8());
        assertEquals(0xBEEF, in.readU16());
        assertEquals(0xDEADBEEFL, in.readU32());
    }

    @Test
    public void writersKeepOnlyTheirOwnWidth() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        final IOOutputStream stream = new IOOutputStream(bytes);
        stream.writeU8(0x1FF);
        stream.writeU16(0x1FFFF);

        assertArrayEquals(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, bytes.toByteArray());
    }

    @Test
    public void textCarriesNoLengthOrTerminator() throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        new IOOutputStream(bytes).writeString("minecraft:redstone");

        assertArrayEquals("minecraft:redstone".getBytes(US_ASCII), bytes.toByteArray());
        assertEquals("minecraft:redstone",
            new IOInputStream(new ByteArrayInputStream(bytes.toByteArray())).readString());
    }

    @Test
    public void textReadsOnlyWhatIsLeft() throws Exception {
        final IOInputStream stream = read('a', 'b', 'c');
        assertEquals('a', stream.readU8());
        assertEquals("bc", stream.readString());
    }

    @Test
    public void textEndsAtATerminator() throws Exception {
        final IOInputStream stream = read('a', 'b', 0, 'c', 'd');
        assertEquals("ab", stream.readString());
        assertEquals("cd", stream.readString(), "the terminator should be consumed, not left for the next read");
    }

    @Test
    public void aTrailingTerminatorIsConsumed() throws Exception {
        final IOInputStream stream = read('a', 0);
        assertEquals("a", stream.readString());
        assertEquals(-1, stream.read());
    }

    @Test
    public void emptyTextIsNotAnError() throws Exception {
        assertEquals("", read().readString());
    }

    @Test
    public void runningOutOfBytesReportsEndOfFile() {
        assertThrows(EOFException.class, () -> read().readU8());
        assertThrows(EOFException.class, () -> read(0x01).readU16());
        assertThrows(EOFException.class, () -> read(0x01, 0x02, 0x03).readU32());
    }

    @Test
    public void plainReadStillYieldsMinusOneAtTheEnd() throws Exception {
        assertEquals(-1, read().read());
    }

    // --------------------------------------------------------------------- //

    private static IOInputStream read(final int... values) {
        final byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return new IOInputStream(new ByteArrayInputStream(bytes));
    }
}

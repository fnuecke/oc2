/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.sedna.cpm.Cpm;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public final class CpmImageTests {
    private static final int DIRECTORY_ENTRY_SIZE = 32;
    private static final int RECORD_SIZE = 128;
    private static final int END_OF_FILE = 0x1A;

    private static final String SHIPPED = "data/oc2/cpm/";

    @Test
    public void composedRomDriveHoldsBothTheEmulatorsFilesAndOurs() throws IOException {
        final Map<String, byte[]> files = listFiles(composeShippedRomDrive());

        assertTrue(files.containsKey("DEVS.COM"), "the emulator's own files must survive: " + files.keySet());
        assertTrue(files.containsKey("DEVLIB.INC"), "the emulator's own files must survive: " + files.keySet());
        assertTrue(files.containsKey("OCAPI.INC"), "oc2 must add its library: " + files.keySet());
        assertTrue(files.containsKey("REDSTN.Z80"), "oc2 must add its example: " + files.keySet());
    }

    @Test
    public void addedFilesReadBackByteForByte() throws IOException {
        final Map<String, byte[]> files = listFiles(composeShippedRomDrive());

        assertContentMatches(SHIPPED + "ocapi.inc", files.get("OCAPI.INC"));
        assertContentMatches(SHIPPED + "redstn.z80", files.get("REDSTN.Z80"));
    }

    @Test
    public void theExampleReferencesTheLibrary() throws IOException {
        final String example = new String(resource(SHIPPED + "redstn.z80"), StandardCharsets.US_ASCII);
        assertTrue(example.contains("OCFIND"), "the example should use the library it ships beside");
        assertTrue(example.contains("OCAPI.INC"), "the example should include the library");
    }

    @Test
    public void tailOfTheLastRecordIsPaddedWithEndOfFile() {
        final byte[] image = blankImage();
        final byte[] content = "hello".getBytes(StandardCharsets.US_ASCII);
        CpmImage.addFile(image, "HELLO.TXT", content);

        final byte[] stored = listFiles(image).get("HELLO.TXT");
        assertEquals(RECORD_SIZE, stored.length, "a short file still occupies one whole record");
        assertArrayEquals(content, Arrays.copyOf(stored, content.length));
        for (int i = content.length; i < stored.length; i++) {
            assertEquals(END_OF_FILE, stored[i] & 0xFF, "byte " + i + " should be the end-of-file pad");
        }
    }

    @Test
    public void fileSpanningSeveralBlocksReadsBack() {
        final byte[] image = blankImage();
        final byte[] content = new byte[Cpm.DiskGeometry.BLOCK_SIZE * 3 + 17];
        for (int i = 0; i < content.length; i++) {
            content[i] = (byte) (i * 31);
        }
        CpmImage.addFile(image, "BIG.DAT", content);

        final byte[] stored = listFiles(image).get("BIG.DAT");
        assertNotNull(stored);
        assertArrayEquals(content, Arrays.copyOf(stored, content.length));
    }

    @Test
    public void severalFilesDoNotOverlap() {
        final byte[] image = blankImage();
        final byte[] first = filled(Cpm.DiskGeometry.BLOCK_SIZE + 1, (byte) 0x11);
        final byte[] second = filled(Cpm.DiskGeometry.BLOCK_SIZE + 1, (byte) 0x22);
        CpmImage.addFile(image, "ONE.DAT", first);
        CpmImage.addFile(image, "TWO.DAT", second);

        final Map<String, byte[]> files = listFiles(image);
        assertArrayEquals(first, Arrays.copyOf(files.get("ONE.DAT"), first.length));
        assertArrayEquals(second, Arrays.copyOf(files.get("TWO.DAT"), second.length));
    }

    @Test
    public void duplicateNameThrows() {
        final byte[] image = blankImage();
        CpmImage.addFile(image, "ONE.DAT", filled(16, (byte) 1));
        assertThrows(IllegalArgumentException.class, () -> CpmImage.addFile(image, "one.dat", filled(16, (byte) 2)));
    }

    @Test
    public void oversizedFileThrows() {
        final byte[] image = blankImage();
        assertThrows(IllegalArgumentException.class,
            () -> CpmImage.addFile(image, "BIG.DAT", filled(CpmImage.MAX_FILE_SIZE + 1, (byte) 1)));
    }

    @Test
    public void malformedNameThrows() {
        final byte[] image = blankImage();
        assertThrows(IllegalArgumentException.class, () -> CpmImage.addFile(image, "TOOLONGNAME.TXT", filled(1, (byte) 1)));
        assertThrows(IllegalArgumentException.class, () -> CpmImage.addFile(image, ".TXT", filled(1, (byte) 1)));
        assertThrows(IllegalArgumentException.class, () -> CpmImage.addFile(image, "NAME.TOOLONG", filled(1, (byte) 1)));
    }

    @Test
    public void wrongSizedImageThrows() {
        assertThrows(IllegalArgumentException.class,
            () -> CpmImage.addFile(new byte[16], "ONE.DAT", filled(1, (byte) 1)));
    }

    // --------------------------------------------------------------------- //

    /**
     * Composes the drive from the files oc2 ships, the same way the reload listener does, but
     * reading them off the classpath so the test needs no resource manager.
     */
    private static byte[] composeShippedRomDrive() throws IOException {
        final Map<String, byte[]> files = new LinkedHashMap<>();
        for (final String name : List.of("ocapi.inc", "redstn.z80")) {
            files.put(name, resource(SHIPPED + name));
        }
        return CpmSystemDisk.compose(files);
    }

    private static void assertContentMatches(final String resource, final byte[] stored) throws IOException {
        final byte[] expected = CpmSystemDisk.toCpmText(resource(resource));
        assertNotNull(stored, resource + " is missing from the image");
        assertArrayEquals(expected, Arrays.copyOf(stored, expected.length), resource + " did not survive");
    }

    private static byte[] resource(final String path) throws IOException {
        try (InputStream stream = CpmImageTests.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(stream, "missing resource " + path);
            return stream.readAllBytes();
        }
    }

    private static byte[] filled(final int length, final byte value) {
        final byte[] content = new byte[length];
        Arrays.fill(content, value);
        return content;
    }

    private static byte[] blankImage() {
        final byte[] image = new byte[Cpm.DiskGeometry.getImageSize()];
        Arrays.fill(image, (byte) 0xE5);
        return image;
    }

    /**
     * Reads a CP/M directory back using the rules the writer claims to follow, so a disagreement
     * between the two shows up as a failing test rather than as a disk CP/M cannot read.
     */
    private static Map<String, byte[]> listFiles(final byte[] image) {
        final int directory = Cpm.DiskGeometry.RESERVED_TRACKS
            * Cpm.DiskGeometry.SECTORS_PER_TRACK * Cpm.DiskGeometry.SECTOR_SIZE;
        final int blockSize = Cpm.DiskGeometry.BLOCK_SIZE;

        final Map<String, byte[]> files = new LinkedHashMap<>();
        for (int i = 0; i < Cpm.DiskGeometry.DIRECTORY_ENTRIES; i++) {
            final int entry = directory + i * DIRECTORY_ENTRY_SIZE;
            if ((image[entry] & 0xFF) != 0x00) {
                continue;
            }

            final String name = text(image, entry + 1, 8);
            final String type = text(image, entry + 9, 3);
            final int records = image[entry + 15] & 0xFF;

            final byte[] content = new byte[records * RECORD_SIZE];
            for (int record = 0; record < records; record++) {
                final int offset = record * RECORD_SIZE;
                final int block = image[entry + 16 + offset / blockSize] & 0xFF;
                System.arraycopy(image, directory + block * blockSize + offset % blockSize,
                    content, offset, RECORD_SIZE);
            }

            files.put(type.isEmpty() ? name : name + "." + type, content);
        }
        return files;
    }

    private static String text(final byte[] image, final int offset, final int length) {
        final StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append((char) (image[offset + i] & 0x7F)); // the high bit is an attribute flag
        }
        return builder.toString().strip();
    }
}

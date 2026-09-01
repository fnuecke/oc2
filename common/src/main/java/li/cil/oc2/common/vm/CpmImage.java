/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.sedna.cpm.Cpm;

import java.util.Locale;

public final class CpmImage {
    private static final int DIRECTORY_ENTRY_SIZE = 32;
    private static final int RECORD_SIZE = 128;
    private static final int RECORDS_PER_EXTENT = 128;
    private static final int ALLOCATION_OFFSET = 16;
    private static final int ALLOCATION_SIZE = 16;
    private static final int NAME_LENGTH = 8;
    private static final int TYPE_LENGTH = 3;

    private static final int USER = 0x00;
    private static final int ENTRY_FREE = 0xE5;

    /**
     * CP/M tracks a file's length in 128 byte records, not bytes; the rest of the last record
     * is padded with the end-of-file character.
     */
    private static final int END_OF_FILE = 0x1A;

    public static final int MAX_FILE_SIZE = RECORDS_PER_EXTENT * RECORD_SIZE;

    // --------------------------------------------------------------------- //

    public static void addFile(final byte[] image, final String fileName, final byte[] content) {
        if (image.length != Cpm.DiskGeometry.getImageSize()) {
            throw new IllegalArgumentException("Image size [" + image.length +
                "] does not match the geometry [" + Cpm.DiskGeometry.getImageSize() + "].");
        }
        if (blockCount() > 0x100) {
            throw new IllegalStateException("The disk has " + blockCount() +
                " blocks; this writer only handles the single byte allocation entries CP/M uses below 256.");
        }
        if (content.length > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File [" + fileName + "] is larger than " + MAX_FILE_SIZE + " bytes.");
        }

        final byte[] name = toEntryName(fileName);
        final int entry = findFreeEntry(image, name, fileName);

        final int blockSize = Cpm.DiskGeometry.BLOCK_SIZE;
        final int blockCount = (content.length + blockSize - 1) / blockSize;
        final int[] blocks = allocate(image, blockCount);

        for (int i = 0; i < blockCount; i++) {
            final int from = i * blockSize;
            System.arraycopy(content, from, image, offsetOf(blocks[i]), Math.min(blockSize, content.length - from));
        }

        final int records = (content.length + RECORD_SIZE - 1) / RECORD_SIZE;
        for (int i = content.length; i < records * RECORD_SIZE; i++) {
            image[offsetOf(blocks[i / blockSize]) + i % blockSize] = (byte) END_OF_FILE;
        }

        image[entry] = USER;
        System.arraycopy(name, 0, image, entry + 1, name.length);
        image[entry + 12] = 0; // EX, the extent within the entry
        image[entry + 13] = 0; // S1, reserved
        image[entry + 14] = 0; // S2, the high bits of the extent
        image[entry + 15] = (byte) records;
        for (int i = 0; i < ALLOCATION_SIZE; i++) {
            image[entry + ALLOCATION_OFFSET + i] = (byte) (i < blockCount ? blocks[i] : 0);
        }
    }

    // --------------------------------------------------------------------- //

    private static int directoryOffset() {
        return Cpm.DiskGeometry.RESERVED_TRACKS * Cpm.DiskGeometry.SECTORS_PER_TRACK * Cpm.DiskGeometry.SECTOR_SIZE;
    }

    private static int offsetOf(final int block) {
        return directoryOffset() + block * Cpm.DiskGeometry.BLOCK_SIZE;
    }

    private static int blockCount() {
        return (Cpm.DiskGeometry.getImageSize() - directoryOffset()) / Cpm.DiskGeometry.BLOCK_SIZE;
    }

    private static int directoryBlocks() {
        final int directorySize = Cpm.DiskGeometry.DIRECTORY_ENTRIES * DIRECTORY_ENTRY_SIZE;
        return (directorySize + Cpm.DiskGeometry.BLOCK_SIZE - 1) / Cpm.DiskGeometry.BLOCK_SIZE;
    }

    private static int[] allocate(final byte[] image, final int count) {
        // The directory occupies the first blocks and never appears in an allocation map, so it
        // has to be excluded explicitly. Block zero doubles as "no block" in an entry, which is
        // exactly why the directory is put there.
        final boolean[] used = new boolean[blockCount()];
        for (int block = 0; block < directoryBlocks(); block++) {
            used[block] = true;
        }

        for (int entry = directoryOffset(); entry < directoryOffset() + Cpm.DiskGeometry.DIRECTORY_ENTRIES * DIRECTORY_ENTRY_SIZE; entry += DIRECTORY_ENTRY_SIZE) {
            if ((image[entry] & 0xFF) == ENTRY_FREE) {
                continue;
            }
            for (int i = 0; i < ALLOCATION_SIZE; i++) {
                final int block = image[entry + ALLOCATION_OFFSET + i] & 0xFF;
                if (block != 0) {
                    used[block] = true;
                }
            }
        }

        final int[] blocks = new int[count];
        int candidate = 0;
        for (int i = 0; i < count; i++) {
            while (candidate < used.length && used[candidate]) {
                candidate++;
            }
            if (candidate == used.length) {
                throw new IllegalStateException("The image has no free blocks left.");
            }
            blocks[i] = candidate;
            used[candidate] = true;
        }
        return blocks;
    }

    private static int findFreeEntry(final byte[] image, final byte[] name, final String fileName) {
        int free = -1;
        for (int entry = directoryOffset(); entry < directoryOffset() + Cpm.DiskGeometry.DIRECTORY_ENTRIES * DIRECTORY_ENTRY_SIZE; entry += DIRECTORY_ENTRY_SIZE) {
            if ((image[entry] & 0xFF) == ENTRY_FREE) {
                if (free < 0) {
                    free = entry;
                }
                continue;
            }

            if (matches(image, entry, name)) {
                throw new IllegalArgumentException("The image already holds a file named [" + fileName + "].");
            }
        }

        if (free < 0) {
            throw new IllegalStateException("The image has no free directory entries left.");
        }
        return free;
    }

    private static boolean matches(final byte[] image, final int entry, final byte[] name) {
        for (int i = 0; i < name.length; i++) {
            // The high bit of a name character is an attribute flag, not part of the name.
            if ((image[entry + 1 + i] & 0x7F) != name[i]) {
                return false;
            }
        }
        return true;
    }

    private static byte[] toEntryName(final String fileName) {
        final String upper = fileName.toUpperCase(Locale.ROOT);
        final int dot = upper.indexOf('.');
        final String name = dot < 0 ? upper : upper.substring(0, dot);
        final String type = dot < 0 ? "" : upper.substring(dot + 1);

        if (name.isEmpty() || name.length() > NAME_LENGTH || type.length() > TYPE_LENGTH || type.indexOf('.') >= 0) {
            throw new IllegalArgumentException("File name [" + fileName + "] is not a CP/M 8.3 name.");
        }

        final byte[] entryName = new byte[NAME_LENGTH + TYPE_LENGTH];
        for (int i = 0; i < entryName.length; i++) {
            final String part = i < NAME_LENGTH ? name : type;
            final int index = i < NAME_LENGTH ? i : i - NAME_LENGTH;
            final char character = index < part.length() ? part.charAt(index) : ' ';
            if (character < 0x20 || character > 0x7E) {
                throw new IllegalArgumentException("File name [" + fileName + "] holds a character CP/M cannot store.");
            }
            entryName[i] = (byte) character;
        }
        return entryName;
    }

    private CpmImage() {
    }
}

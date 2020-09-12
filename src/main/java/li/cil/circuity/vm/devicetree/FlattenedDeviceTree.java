package li.cil.circuity.vm.devicetree;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.ByteArrayOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class FlattenedDeviceTree {
    private static final int FDT_MAGIC = 0xd00dfeed;
    private static final int FDT_VERSION = 0x11;
    private static final int FDT_LAST_COMP_VERSION = 0x10;

    private static final int FDT_BEGIN_NODE = 0x00000001;
    private static final int FDT_END_NODE = 0x00000002;
    private static final int FDT_PROP = 0x00000003;
    private static final int FDT_NOP = 0x00000004;
    private static final int FDT_END = 0x00000009;

    private static final int FDT_HEADER_SIZE = 10 * 4; // 10 * sizeof(int)

    private List<String> names = new ArrayList<>();
    private IntList nameOffsets = new IntArrayList();

    private final ByteArrayOutputStream structureBlock = new ByteArrayOutputStream();
    private final DataOutputStream structureBlockWriter = new DataOutputStream(structureBlock);
    private int openStructureNodes = 0;

    public void beginNode(final String name) {
        try {
            structureBlockWriter.writeInt(FDT_BEGIN_NODE);
            structureBlockWriter.writeBytes(name);
            structureBlockWriter.writeByte(0);
            pad(structureBlockWriter, 4);
            openStructureNodes++;
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    public void endNode() {
        try {
            structureBlockWriter.writeInt(FDT_END_NODE);
            openStructureNodes--;
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    public void property(final String name, final Object... values) {
        try {
            int valuesByteLength = 0;
            for (final Object value : values) {
                if (value instanceof String) {
                    valuesByteLength += ((String) value).length() + 1;
                } else if (value instanceof Integer) {
                    valuesByteLength += 4;
                } else if (value instanceof Long) {
                    valuesByteLength += 8;
                } else {
                    throw new IllegalArgumentException();
                }
            }

            structureBlockWriter.writeInt(FDT_PROP);
            structureBlockWriter.writeInt(valuesByteLength);
            structureBlockWriter.writeInt(getPropertyNameOffset(name));

            for (final Object value : values) {
                if (value instanceof String) {
                    structureBlockWriter.writeBytes((String) value);
                    structureBlockWriter.writeByte(0);
                } else if (value instanceof Integer) {
                    structureBlockWriter.writeInt((int) value);
                } else if (value instanceof Long) {
                    structureBlockWriter.writeInt((int) (((long) value) >>> 32));
                    structureBlockWriter.writeInt((int) ((long) value));
                } else {
                    throw new AssertionError();
                }
            }

            pad(structureBlockWriter, 4);
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    public byte[] toDTB() {
        if (openStructureNodes != 0) {
            throw new IllegalStateException("Unbalanced nodes.");
        }

        try {
            final ByteArrayOutputStream fdt = new ByteArrayOutputStream();
            final DataOutputStream fdtWriter = new DataOutputStream(fdt);

            // We write blocks in this order:
            // - Memory reservation block
            // - Structure block
            // - Strings block

            // We need to compute the offsets and sizes of these blocks in advance for the header.
            int pos = FDT_HEADER_SIZE;

            final int headerPadding = paddingTo(FDT_HEADER_SIZE, 8);
            pos += headerPadding;

            final int memoryReservationBlockOffset = pos;
            final int memoryReservationBlockLength = 8 + 8; // single 0L, 0L reservation struct.
            pos += memoryReservationBlockLength;

            final int structureBlockOffset = pos;
            final int structureBlockLength = structureBlockWriter.size() + 4; // + 4 for FDT_END
            pos += structureBlockLength;

            final int stringsBlockOffset = pos;
            final int stringsBlockLength = stringsBlockLength();
            pos += stringsBlockLength;

            final fdt_header header = new fdt_header();
            header.boot_cpuid_phys = 0;
            header.totalsize = pos;
            header.size_dt_strings = stringsBlockLength;
            header.size_dt_struct = structureBlockLength;
            header.off_dt_struct = structureBlockOffset;
            header.off_dt_strings = stringsBlockOffset;
            header.off_mem_rsvmap = memoryReservationBlockOffset;
            header.write(fdtWriter);

            pad(fdtWriter, 8);

            fdtWriter.writeLong(0); // fdt_reserve_entry.address
            fdtWriter.writeLong(0); // fdt_reserve_entry.size

            fdtWriter.write(structureBlock.toByteArray());
            fdtWriter.writeInt(FDT_END);

            for (final String name : names) {
                fdtWriter.writeBytes(name);
                fdtWriter.writeByte(0);
            }

            return fdt.toByteArray();
        } catch (final IOException e) {
            throw new AssertionError(e);
        }
    }

    private void pad(final DataOutputStream output, final int alignment) throws IOException {
        final int padding = paddingTo(output.size(), alignment);
        for (int i = 0; i < padding; i++) {
            output.writeByte(0);
        }
    }

    private int paddingTo(final int size, final int alignment) {
        final int mask = alignment - 1;
        if ((size & mask) == 0) {
            return 0;
        } else {
            return ((size & ~mask) + alignment) - size;
        }
    }

    private int getPropertyNameOffset(final String name) {
        final int index = names.indexOf(name);
        if (index >= 0) {
            return nameOffsets.getInt(index);
        } else if (names.size() > 0) {
            final int nameOffset = stringsBlockLength();

            names.add(name);
            nameOffsets.add(nameOffset);

            return nameOffset;
        } else {
            names.add(name);
            nameOffsets.add(0);
            return 0;
        }
    }

    private int stringsBlockLength() {
        if (nameOffsets.size() == 0) {
            return 0;
        }

        final int lastNameIndex = names.size() - 1;
        final int lastNameLength = names.get(lastNameIndex).length();
        final int lastNameOffset = nameOffsets.getInt(lastNameIndex);
        return lastNameOffset + lastNameLength + 1; // +1 for \0 separator
    }

    private static final class fdt_header {
        public final int magic = FDT_MAGIC;
        public int totalsize;
        public int off_dt_struct;
        public int off_dt_strings;
        public int off_mem_rsvmap;
        public final int version = FDT_VERSION;
        public final int last_comp_version = FDT_LAST_COMP_VERSION;
        public int boot_cpuid_phys;
        public int size_dt_strings;
        public int size_dt_struct;

        public void write(final DataOutput output) throws IOException {
            output.writeInt(magic);
            output.writeInt(totalsize);
            output.writeInt(off_dt_struct);
            output.writeInt(off_dt_strings);
            output.writeInt(off_mem_rsvmap);
            output.writeInt(version);
            output.writeInt(last_comp_version);
            output.writeInt(boot_cpuid_phys);
            output.writeInt(size_dt_strings);
            output.writeInt(size_dt_struct);
        }
    }
}

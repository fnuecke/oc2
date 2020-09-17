package li.cil.circuity.vm.elf;

import java.nio.ByteBuffer;
import java.util.List;

public final class ELF {
    public ByteBuffer data;

    public int version;
    public Format format;
    public Endianness endianness;
    public int type;
    public int isa;
    public int abi;
    public int abiVersion;
    public int flags;
    public long entryPoint;
    public int sectionNameEntryIndex;
    public int headerSize;

    public long programHeaderTableOffset;
    public int programHeaderTableEntrySize;
    public int programHeaderTableEntryCount;
    public List<ProgramHeader> programHeaderTable;

    public long sectionHeaderTableOffset;
    public int sectionHeaderTableEntrySize;
    public int sectionHeaderTableEntryCount;
    public List<SectionHeader> sectionHeaderTable;

    public boolean is(final Type type) {
        return this.type == type.value;
    }

    public boolean is(final ISA isa) {
        return this.isa == isa.value;
    }

    public boolean is(final ABI abi) {
        return this.abi == abi.value;
    }
}

package li.cil.circuity.vm.elf;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;

public final class ProgramHeader {
    public final ELF elf;

    public int type;
    public int flags;

    public long offset;
    public long sizeInFile;

    public long virtualAddress;
    public long physicalAddress;
    public long sizeInMemory;
    public long alignment;

    public ProgramHeader(final ELF elf) {
        this.elf = elf;
    }

    public ByteBuffer getView() {
        final int position = elf.data.position();
        final int limit = elf.data.limit();

        elf.data.limit((int) (offset + sizeInFile));
        elf.data.position((int) offset);

        final ByteBuffer result = elf.data.slice();

        elf.data.limit(limit);
        elf.data.position(position);

        return result;
    }

    public boolean is(final ProgramHeaderType type) {
        return this.type == type.value;
    }

    @Nullable
    public ProgramHeaderType getType() {
        for (final ProgramHeaderType type : ProgramHeaderType.values()) {
            if (is(type)) {
                return type;
            }
        }

        return null;
    }

    @Override
    public String toString() {
        final ProgramHeaderType type = getType();
        return "ProgramHeader{" +
               "type = " + (type != null ? type : ("0x" + Integer.toHexString(this.type))) +
               ((type != ProgramHeaderType.PT_NULL) ? (
                       ", offset=0x" + Long.toHexString(offset) +
                       ", virtualAddress=0x" + Long.toHexString(virtualAddress) +
                       ", physicalAddress=0x" + Long.toHexString(physicalAddress) +
                       ", sizeInFile=0x" + Long.toHexString(sizeInFile) +
                       ", sizeInMemory=0x" + Long.toHexString(sizeInMemory) +
                       (alignment > 1 ? (", alignment=0x" + Long.toHexString(alignment)) : "")
               ) : "") +
               '}';
    }
}

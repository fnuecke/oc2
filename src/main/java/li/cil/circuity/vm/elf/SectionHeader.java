package li.cil.circuity.vm.elf;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

final class SectionHeader {
    public final ELF elf;

    public String name;
    public int nameOffset;
    public int type;
    public long flags;

    public long offset;
    public long size;

    public long virtualAddress;
    public long alignment;
    public int link;
    public int info;
    public long entrySize;

    SectionHeader(final ELF elf) {
        this.elf = elf;
    }

    public ByteBuffer getView() {
        final int position = elf.data.position();
        final int limit = elf.data.limit();

        elf.data.limit((int) (offset + size));
        elf.data.position((int) offset);

        final ByteBuffer result = elf.data.slice();

        elf.data.limit(limit);
        elf.data.position(position);

        return result;
    }

    public boolean is(final SectionHeaderType type) {
        return this.type == type.value;
    }

    @Nullable
    public SectionHeaderType getType() {
        for (final SectionHeaderType type : SectionHeaderType.values()) {
            if (is(type)) {
                return type;
            }
        }

        return null;
    }

    public boolean has(final SectionHeaderFlags flag) {
        return (this.flags & flag.value) != 0;
    }

    public Collection<SectionHeaderFlags> getFlags() {
        final ArrayList<SectionHeaderFlags> result = new ArrayList<>();
        for (final SectionHeaderFlags flag : SectionHeaderFlags.values()) {
            if (has(flag)) {
                result.add(flag);
            }
        }
        return result;
    }

    @Override
    public String toString() {
        final SectionHeaderType type = getType();
        final Collection<SectionHeaderFlags> flags = getFlags();
        return "SectionHeader{" +
               "name=" + (name != null ? name : nameOffset) +
               ", type=" + (type != null ? type : ("0x" + Integer.toHexString(this.type))) +
               ((type != SectionHeaderType.SHT_NULL) ? (
                       (!flags.isEmpty() ? (", flags =" + flags.stream().map(Enum::toString).collect(Collectors.joining(" | "))) : "") +
                       ", virtualAddress=0x" + Long.toHexString(virtualAddress) +
                       ", offset=0x" + Long.toHexString(offset) +
                       ", size=0x" + Long.toHexString(size) +
                       (link > 0 ? (", link=" + link) : "") +
                       (info > 0 ? (", info=" + info) : "") +
                       (alignment > 1 ? (", alignment=0x" + Long.toHexString(alignment)) : "") +
                       (entrySize > 0 ? (", entrySize=0x" + Long.toHexString(entrySize)) : "")
               ) : "") +
               '}';
    }
}

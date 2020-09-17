package li.cil.circuity.vm.elf;

import org.apache.commons.io.IOUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public final class ELFParser {
    private static final byte[] EI_MAG = {0x7F, 'E', 'L', 'F'};

    public static ELF parse(final String path) throws IOException {
        try (final FileInputStream fis = new FileInputStream(path)) {
            return parse(new BufferedInputStream(fis));
        }
    }

    public static ELF parse(final InputStream stream) throws IOException {
        return parse(IOUtils.toByteArray(stream));
    }

    public static ELF parse(final byte[] data) {
        final ELF elf = new ELF();

        elf.data = ByteBuffer.wrap(data);
        readHeader(elf);
        readTables(elf);

        final SectionHeader nameSection = elf.sectionHeaderTable.get(elf.sectionNameEntryIndex);
        if (!nameSection.is(SectionHeaderType.SHT_STRTAB)) {
            throw new IllegalArgumentException("name section is not of type SHT_STRTAB");
        }

        final long nameListBase = nameSection.offset;
        for (final SectionHeader sectionHeader : elf.sectionHeaderTable) {
            final long nameStart = nameListBase + sectionHeader.nameOffset;
            elf.data.position((int) nameStart);
            final StringBuilder sb = new StringBuilder();
            char ch;
            while ((ch = (char) elf.data.get()) != '\0') {
                sb.append(ch);
            }
            sectionHeader.name = sb.toString();
        }

        return elf;
    }

    private static byte read(final ELF elf) {
        return elf.data.get();
    }

    private static int readi(final ELF elf) {
        return read(elf) & 0xFF;
    }

    private static short read16(final ELF elf) {
        switch (elf.endianness) {
            case LITTLE_ENDIAN:
                return (short) (readi(elf) | (readi(elf) << 8));
            case BIG_ENDIAN:
                return (short) ((readi(elf) << 8) | readi(elf));
            default:
                throw new IllegalArgumentException();
        }
    }

    private static int read16i(final ELF elf) {
        return read16(elf) & 0xFFFF;
    }

    private static int read32(final ELF elf) {
        switch (elf.endianness) {
            case LITTLE_ENDIAN:
                return read16i(elf) | (read16i(elf) << 16);
            case BIG_ENDIAN:
                return (read16i(elf) << 16) | read16i(elf);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static long read32l(final ELF elf) {
        return ((long) read32(elf)) & 0xFFFFFFFFL;
    }

    private static long read64(final ELF elf) {
        switch (elf.endianness) {
            case LITTLE_ENDIAN:
                return read32l(elf) | (read32l(elf) << 32);
            case BIG_ENDIAN:
                return (read32l(elf) << 32) | read32l(elf);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static long readWord(final ELF elf) {
        switch (elf.format) {
            case x32:
                return read32l(elf);
            case x64:
                return read64(elf);
            default:
                throw new IllegalArgumentException();
        }
    }

    private static void skip(final ELF elf, final long count) {
        for (int i = 0; i < count; i++) {
            read(elf);
        }
    }

    private static void readHeader(final ELF elf) {
        // e_ident[EI_MAG0..EI_MAG3]
        for (int i = 0; i < EI_MAG.length; i++) {
            if (read(elf) != EI_MAG[i]) {
                throw new IllegalArgumentException("invalid ELF header");
            }
        }

        // e_ident[EI_CLASS]
        switch (read(elf)) {
            case 1:
                elf.format = Format.x32;
                break;
            case 2:
                elf.format = Format.x64;
                break;
            default:
                throw new IllegalArgumentException("invalid bit format");
        }

        // e_ident[EI_DATA]
        switch (read(elf)) {
            case 1:
                elf.endianness = Endianness.LITTLE_ENDIAN;
                break;
            case 2:
                elf.endianness = Endianness.BIG_ENDIAN;
                break;
            default:
                throw new IllegalArgumentException("invalid endianness");
        }

        // e_ident[EI_VERSION]
        final int headerVersion = read(elf);
        if (headerVersion != 1) {
            throw new IllegalArgumentException("invalid ELF header version");
        }

        // e_ident[EI_OSABI]
        elf.abi = readi(elf);

        // e_ident[EI_ABIVERSION]
        elf.abiVersion = readi(elf);

        // e_ident[EI_PAD]
        skip(elf, 7);

        // e_type
        elf.type = read16i(elf);

        // e_machine
        elf.isa = read16i(elf);

        // e_version
        elf.version = read32(elf);
        if (elf.version != 1) {
            throw new IllegalArgumentException("invalid ELF version");
        }

        // e_entry
        elf.entryPoint = readWord(elf);

        // e_phoff
        elf.programHeaderTableOffset = readWord(elf);

        // e_shoff
        elf.sectionHeaderTableOffset = readWord(elf);

        // e_flags
        elf.flags = read32(elf);

        // e_ehsize
        elf.headerSize = read16i(elf);
        final int minHeaderSize;
        switch (elf.format) {
            case x32:
                minHeaderSize = 0x34;
                break;
            case x64:
                minHeaderSize = 0x40;
                break;
            default:
                throw new AssertionError();
        }

        if (elf.headerSize < minHeaderSize) {
            throw new IllegalArgumentException("invalid header size");
        }

        if (elf.programHeaderTableOffset < elf.headerSize) {
            throw new IllegalArgumentException("program header table intersects header");
        }

        if (elf.sectionHeaderTableOffset < elf.headerSize) {
            throw new IllegalArgumentException("section header table intersects header");
        }

        if (elf.programHeaderTableOffset == elf.sectionHeaderTableOffset) {
            throw new IllegalArgumentException("program header table and section header table start at same offset");
        }

        // e_phentsize
        elf.programHeaderTableEntrySize = read16i(elf);

        // e_phnum
        elf.programHeaderTableEntryCount = read16i(elf);

        // e_shentsize
        elf.sectionHeaderTableEntrySize = read16i(elf);

        // e_shnum
        elf.sectionHeaderTableEntryCount = read16i(elf);

        if (elf.programHeaderTableOffset < elf.sectionHeaderTableOffset &&
            elf.programHeaderTableOffset + elf.programHeaderTableEntrySize * elf.programHeaderTableEntryCount > elf.sectionHeaderTableOffset) {
            throw new IllegalArgumentException("program header table intersects section header table");
        } else if (elf.sectionHeaderTableOffset < elf.programHeaderTableOffset &&
                   elf.sectionHeaderTableOffset + elf.sectionHeaderTableEntrySize * elf.sectionHeaderTableEntryCount > elf.programHeaderTableOffset) {
            throw new IllegalArgumentException("section header table intersects program header table");
        }

        // e_shstrndx
        elf.sectionNameEntryIndex = read16i(elf);

        if (elf.sectionNameEntryIndex >= elf.sectionHeaderTableEntryCount) {
            throw new IllegalArgumentException("invalid section name entry index");
        }

        skip(elf, elf.headerSize - minHeaderSize);
    }

    private static void readTables(final ELF elf) {
        elf.programHeaderTable = readProgramHeaderTable(elf);
        elf.sectionHeaderTable = readSectionHeaderTable(elf);
    }

    private static List<ProgramHeader> readProgramHeaderTable(final ELF elf) {
        elf.data.position((int) elf.programHeaderTableOffset);

        final int minEntrySize;
        switch (elf.format) {
            case x32:
                minEntrySize = 0x20;
                break;
            case x64:
                minEntrySize = 0x38;
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (elf.programHeaderTableEntrySize < minEntrySize) {
            throw new IllegalArgumentException("invalid program header size");
        }

        final ArrayList<ProgramHeader> result = new ArrayList<>();

        for (int i = 0; i < elf.programHeaderTableEntryCount; i++) {
            final ProgramHeader header = new ProgramHeader(elf);

            // p_type
            header.type = read32(elf);

            // p_flags if 64 bit
            if (elf.format == Format.x64) {
                header.flags = read32(elf);
            }

            // p_offset
            header.offset = readWord(elf);

            // p_vaddr
            header.virtualAddress = readWord(elf);

            // p_paddr
            header.physicalAddress = readWord(elf);

            // p_filesz
            header.sizeInFile = readWord(elf);

            // p_memsz
            header.sizeInMemory = readWord(elf);

            // p_flags if 32 bit
            if (elf.format == Format.x32) {
                header.flags = read32(elf);
            }

            // p_align
            header.alignment = readWord(elf);

            if (Long.compareUnsigned(header.alignment, 1) > 0) {
                if (Long.bitCount(header.alignment) != 1) {
                    throw new IllegalArgumentException("invalid alignment of program: not a power of two");
                }
                if ((header.virtualAddress & (header.alignment - 1)) != (header.offset & (header.alignment - 1))) {
                    throw new IllegalArgumentException("invalid alignment of program: p_vaddr and p_offset misaligned");
                }
            }

            skip(elf, elf.programHeaderTableEntrySize - minEntrySize);

            result.add(header);
        }

        return result;
    }

    private static List<SectionHeader> readSectionHeaderTable(final ELF elf) {
        elf.data.position((int) elf.sectionHeaderTableOffset);

        final int minEntrySize;
        switch (elf.format) {
            case x32:
                minEntrySize = 0x28;
                break;
            case x64:
                minEntrySize = 0x40;
                break;
            default:
                throw new IllegalArgumentException();
        }

        if (elf.sectionHeaderTableEntrySize < minEntrySize) {
            throw new IllegalArgumentException("invalid section header size");
        }

        final ArrayList<SectionHeader> result = new ArrayList<>();

        for (int i = 0; i < elf.sectionHeaderTableEntryCount; i++) {
            final SectionHeader header = new SectionHeader(elf);

            // sh_name
            header.nameOffset = read32(elf);

            // sh_type
            header.type = read32(elf);

            // sh_flags
            header.flags = readWord(elf);

            // sh_addr
            header.virtualAddress = readWord(elf);

            // sh_offset
            header.offset = readWord(elf);

            // sh_size
            header.size = readWord(elf);

            // sh_link
            header.link = read32(elf);

            // sh_info
            header.info = read32(elf);

            // sh_addralign
            header.alignment = readWord(elf);

            if (Long.compareUnsigned(header.alignment, 1) > 0 && Long.bitCount(header.alignment) != 1) {
                throw new IllegalArgumentException("invalid alignment of section: not a power of two");
            }

            // sh_entsize
            header.entrySize = readWord(elf);

            skip(elf, elf.sectionHeaderTableEntrySize - minEntrySize);

            result.add(header);
        }

        return result;
    }
}

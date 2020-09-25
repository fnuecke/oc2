package li.cil.circuity.vm.riscv;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.SimpleMemoryMap;
import li.cil.circuity.vm.device.memory.Memory;
import li.cil.circuity.vm.elf.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ISATests {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final String[] TEST_FILTERS = {
            "rv32mi-p-.*",
            "rv32si-p-.*",
            "rv32ua-p-.*",
            "rv32uc-p-.*",
//            "rv32ud-p-.*",
//            "rv32uf-p-.*",
            "rv32ui-p-.*",
            "rv32um-p-.*",

            "rv32ua-v-.*",
            "rv32uc-v-.*",
//            "rv32ud-v-.*",
//            "rv32uf-v-.*",
            "rv32ui-v-.*",
            "rv32um-v-.*",
    };

    private static final int PHYSICAL_MEMORY_START = 0x80000000;
    private static final int PHYSICAL_MEMORY_LENGTH = 32 * 1024 * 1024;

    @TestFactory
    public Collection<DynamicTest> testISA() {
        final File[] testFiles = new File("src/test/data/riscv-tests").listFiles();
        Assertions.assertNotNull(testFiles);
        return Arrays.stream(testFiles)
                .filter(File::isFile)
                .map(file -> {
                    final String filter = getMatchingFilter(file);
                    if (filter == null) {
                        LOGGER.info("No filter matches file [{}], skipping.", file.getName());
                        return null;
                    }

                    return DynamicTest.dynamicTest(file.getName(), () -> {
                        LOGGER.info("Running test for file [{}].", file.getName());

                        final ELF elf = ELFParser.parse(file);

                        final int toHostAddress = getToHostAddress(elf);

                        final MemoryMap memoryMap = new SimpleMemoryMap();
                        final R5CPU cpu = new R5CPU(memoryMap);
                        final HostTargetInterface htif = new HostTargetInterface();

                        // RAM block below and potentially up to HTIF.
                        if (PHYSICAL_MEMORY_START < toHostAddress) {
                            final int end = Math.min(PHYSICAL_MEMORY_START + PHYSICAL_MEMORY_LENGTH, toHostAddress);
                            memoryMap.addDevice(PHYSICAL_MEMORY_START, Memory.create(end - PHYSICAL_MEMORY_START));
                        }

                        // RAM block above and potentially starting from HTIF.
                        if (PHYSICAL_MEMORY_START + PHYSICAL_MEMORY_LENGTH > toHostAddress + htif.getLength()) {
                            final int start = Math.max(PHYSICAL_MEMORY_START, toHostAddress + htif.getLength());
                            memoryMap.addDevice(start, Memory.create(PHYSICAL_MEMORY_START + PHYSICAL_MEMORY_LENGTH - start));
                        }

                        loadProgramSegments(elf, memoryMap);

                        memoryMap.addDevice(toHostAddress, htif);

                        cpu.reset(true, (int) elf.entryPoint);

                        Assertions.assertThrows(TestSuccessful.class, () -> {
                            for (int i = 0; i < 1_000_000; i++) {
                                cpu.step(1_000);
                            }
                        });
                    });
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private int getToHostAddress(final ELF elf) {
        for (final SectionHeader header : elf.sectionHeaderTable) {
            if (".tohost".equals(header.name)) {
                return (int) header.virtualAddress;
            }
        }

        Assertions.fail(".tohost not found in ELF");
        return 0; // appeasing the compiler: this line will never be executed.
    }

    private void loadProgramSegments(final ELF elf, final MemoryMap memoryMap) throws MemoryAccessException {
        for (final ProgramHeader header : elf.programHeaderTable) {
            if (header.is(ProgramHeaderType.PT_LOAD)) {
                final ByteBuffer data = header.getView();
                final int address = (int) header.physicalAddress;
                final int length = (int) header.sizeInFile;
                for (int i = 0; i < length; i++) {
                    memoryMap.store(address + i, data.get(), Sizes.SIZE_8_LOG2);
                }
            }
        }
    }

    @Nullable
    private static String getMatchingFilter(final File file) {
        for (final String filter : TEST_FILTERS) {
            if (file.getName().matches(filter)) {
                return filter;
            }
        }
        return null;
    }

    private static class HostTargetInterface implements MemoryMappedDevice {
        protected long toHost, fromHost;

        @Override
        public int getLength() {
            return 0x48;
        }

        @Override
        public int load(final int offset, final int sizeLog2) {
            assert sizeLog2 == Sizes.SIZE_32_LOG2;
            switch (offset) {
                case 0x00: {
                    return (int) toHost;
                }
                case 0x04: {
                    return (int) (toHost >> 32);
                }

                case 0x40: {
                    return (int) fromHost;
                }
                case 0x44: {
                    return (int) (fromHost >> 32);
                }
            }

            return 0;
        }

        @Override
        public void store(final int offset, final int value, final int sizeLog2) {
            assert sizeLog2 == Sizes.SIZE_32_LOG2;
            switch (offset) {
                case 0x00: {
                    toHost = (toHost & ~0xFFFFFFFFL) | value;
                    handleCommand();
                    break;
                }
                case 0x04: {
                    toHost = (toHost & 0xFFFFFFFFL) | ((long) value << 32);
                    handleCommand();
                    break;
                }

                case 0x40: {
                    fromHost = (fromHost & ~0xFFFFFFFFL) | value;
                    break;
                }
                case 0x44: {
                    fromHost = (fromHost & 0xFFFFFFFFL) | ((long) value << 32);
                    break;
                }
            }
        }

        protected void handleCommand() {
            if (toHost != 0) {
                final int exitcode = (int) (toHost >>> 1);
                if (exitcode != 0) {
                    Assertions.fail("Test failed with exit code [" + exitcode + "].");
                } else {
                    throw new TestSuccessful();
                }
            }
        }
    }

    private static final class TestSuccessful extends RuntimeException {
    }
}

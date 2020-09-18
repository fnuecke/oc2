package li.cil.circuity.vm.riscv;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.SimpleMemoryMap;
import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import li.cil.circuity.vm.elf.ELF;
import li.cil.circuity.vm.elf.ELFParser;
import li.cil.circuity.vm.elf.ProgramHeader;
import li.cil.circuity.vm.elf.ProgramHeaderType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import javax.annotation.Nullable;
import javax.annotation.RegEx;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class ISATests {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final TestFilter[] TEST_FILTERS = {
            new TestFilter("rv32mi-p-.*", ISATests::handleTestResultP),
            new TestFilter("rv32si-p-.*", ISATests::handleTestResultP),
            new TestFilter("rv32ua-p-.*", ISATests::handleTestResultP),
            new TestFilter("rv32uc-p-.*", ISATests::handleTestResultP),
//            new TestFilter("rv32ud-p-.*", ISATests::handleTestResultP),
//            new TestFilter("rv32uf-p-.*", ISATests::handleTestResultP),
            new TestFilter("rv32ui-p-.*", ISATests::handleTestResultP),
            new TestFilter("rv32um-p-.*", ISATests::handleTestResultP),

            new TestFilter("rv32mi-v-.*", ISATests::handleTestResultV),
            new TestFilter("rv32si-v-.*", ISATests::handleTestResultV),
            new TestFilter("rv32ua-v-.*", ISATests::handleTestResultV),
            new TestFilter("rv32uc-v-.*", ISATests::handleTestResultV),
//            new TestFilter("rv32ud-v-.*", ISATests::handleTestResultV),
//            new TestFilter("rv32uf-v-.*", ISATests::handleTestResultV),
            new TestFilter("rv32ui-v-.*", ISATests::handleTestResultV),
            new TestFilter("rv32um-v-.*", ISATests::handleTestResultV),
    };

    private static final int PHYSICAL_MEMORY_START = 0x80000000;

    private MemoryMap memoryMap;

    @BeforeEach
    public void initialize() {
        memoryMap = new SimpleMemoryMap();
        memoryMap.addDevice(PHYSICAL_MEMORY_START, new ByteBufferMemory(32 * 1014 * 1024));
    }

    @TestFactory
    public Collection<DynamicTest> testISA() {
        final File[] testFiles = new File("src/test/data/riscv-tests").listFiles();
        Assertions.assertNotNull(testFiles);
        return Arrays.stream(testFiles)
                .filter(File::isFile)
                .map(file -> {
                    final TestFilter filter = getMatchingFilter(file);
                    if (filter == null) {
                        return null;
                    }

                    final R5CPU cpu = new R5CPU(memoryMap) {
                        @Override
                        protected void raiseException(final int exception, final int value) {
                            switch (exception) {
                                case R5.EXCEPTION_USER_ECALL:
                                case R5.EXCEPTION_SUPERVISOR_ECALL:
                                case R5.EXCEPTION_MACHINE_ECALL:
                                    filter.ecallHandler.accept(getState());
                                    break;
                            }
                            super.raiseException(exception, value);
                        }
                    };

                    return DynamicTest.dynamicTest(file.getName(), () -> {
                        LOGGER.info("Running test for file [{}]", file.getName());

                        final ELF elf = ELFParser.parse(file);

                        for (final ProgramHeader programHeader : elf.programHeaderTable) {
                            if (programHeader.is(ProgramHeaderType.PT_LOAD)) {
                                final ByteBuffer data = programHeader.getView();
                                final int address = (int) programHeader.physicalAddress;
                                final int length = (int) programHeader.sizeInFile;
                                for (int i = 0; i < length; i++) {
                                    memoryMap.store(address + i, data.get(), Sizes.SIZE_8_LOG2);
                                }
                            }
                        }

                        cpu.reset(true, (int) elf.entryPoint);

                        Assertions.assertThrows(TestSuccessful.class, () -> {
                            for (int i = 0; i < 100_000; i++) {
                                cpu.step(1_000);
                            }
                        });
                    });
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Nullable
    private static TestFilter getMatchingFilter(final File file) {
        for (final TestFilter filter : TEST_FILTERS) {
            if (filter.matches(file)) {
                return filter;
            }
        }
        return null;
    }

    private static void handleTestResultP(final R5CPUStateSnapshot state) {
        final int testResult = state.x[10]; // a0
        if ((testResult & 1) != 0) {
            final int failedTest = testResult >> 1;
            Assertions.fail("test [" + failedTest + "] failed after [" + state.mcycle + "] cycles");
        } else {
            throw new TestSuccessful();
        }
    }

    private static void handleTestResultV(final R5CPUStateSnapshot state) {
        final int testResult = state.x[10]; // a0
        if ((testResult & 1) != 0) {
            final int failedTest = testResult >> 1;
            if (failedTest != 0) {
                Assertions.fail("test [" + failedTest + "] failed after [" + state.mcycle + "] cycles");
            } else {
                throw new TestSuccessful();
            }
        }
    }

    private static final class TestFilter {
        @RegEx final String filter;
        final Consumer<R5CPUStateSnapshot> ecallHandler;

        TestFilter(@RegEx final String filter, final Consumer<R5CPUStateSnapshot> ecallHandler) {
            this.filter = filter;
            this.ecallHandler = ecallHandler;
        }

        boolean matches(final File file) {
            return file.getName().matches(filter);
        }
    }

    private static final class TestSuccessful extends RuntimeException {
    }
}

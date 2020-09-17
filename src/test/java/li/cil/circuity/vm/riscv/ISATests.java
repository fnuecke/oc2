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

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public final class ISATests {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int PHYSICAL_MEMORY_START = 0x80000000;

    private MemoryMap memoryMap;
    private R5CPU cpu;

    @BeforeEach
    public void initialize() {
        memoryMap = new SimpleMemoryMap();

        cpu = new R5CPU(memoryMap) {
            @Override
            protected void raiseException(final int exception, final int value) {
                switch (exception) {
                    case R5.EXCEPTION_USER_ECALL:
                    case R5.EXCEPTION_SUPERVISOR_ECALL:
                    case R5.EXCEPTION_MACHINE_ECALL:
                        final int testResult = getState().x[10]; // a0
                        if ((testResult & 1) != 0) {
                            Assertions.fail("test " + (testResult >> 1) + " failed");
                        } else {
                            throw new TestSuccessful();
                        }
                        break;
                }
                super.raiseException(exception, value);
            }
        };

        memoryMap.addDevice(PHYSICAL_MEMORY_START, new ByteBufferMemory(32 * 1014 * 1024));
    }

    @TestFactory
    public Collection<DynamicTest> testAllFiles() {
        final File[] testFiles = new File("src/test/data/riscv-tests").listFiles();
        Assertions.assertNotNull(testFiles);
        return Arrays.stream(testFiles).filter(File::isFile).map(file ->
                DynamicTest.dynamicTest(file.getName(), () -> {
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
                })).collect(Collectors.toList());
    }

    private static final class TestSuccessful extends RuntimeException {
    }
}

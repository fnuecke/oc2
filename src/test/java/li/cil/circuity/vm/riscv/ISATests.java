package li.cil.circuity.vm.riscv;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.SimpleMemoryMap;
import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import li.cil.circuity.vm.elf.ELF;
import li.cil.circuity.vm.elf.ELFParser;
import li.cil.circuity.vm.elf.ProgramHeader;
import li.cil.circuity.vm.elf.ProgramHeaderType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class ISATests {
    private static final int PHYSICAL_MEMORY_START = 0x80000000;

    private MemoryMap memoryMap;
    private R5CPU cpu;
    private PhysicalMemory memory;

    @BeforeEach
    public void initialize() {
        memoryMap = new SimpleMemoryMap();
        cpu = new R5CPU(memoryMap);
        memory = new ByteBufferMemory(32 * 1014 * 1024);
        memoryMap.addDevice(PHYSICAL_MEMORY_START, memory);
    }

    @Test
    public void testIllegalInst() throws IOException, MemoryAccessException {
        final String elfPath = "buildroot\\target\\share\\riscv-tests\\isa\\rv32mi-p-illegal";
        final ELF elf = ELFParser.parse(elfPath);

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

        cpu.setEcallInterceptor((cpu, e) -> {
            final R5CPUStateSnapshot state = cpu.getState();
            final int testResult = state.x[10];
            if ((testResult & 1) != 0) {
                // fail
                Assertions.fail("ISA test failed: " + (testResult >> 1));
            }
            return true; // Sit out remaining cycles.
        });

        cpu.reset(true, (int) elf.entryPoint);

        for (int i = 0; i < 10000; i++) {
            cpu.step(100);
        }
    }
}

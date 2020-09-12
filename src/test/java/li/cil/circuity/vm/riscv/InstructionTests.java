package li.cil.circuity.vm.riscv;

import li.cil.circuity.api.vm.device.memory.MemoryAccessException;
import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public class InstructionTests {
    static final Logger LOGGER = LogManager.getLogger();

    PhysicalMemory memory;

    R5Board board;

    @BeforeEach
    public void initialize() {
        board = new R5Board();

        memory = new ByteBufferMemory(64 * 1014 * 1024);

        board.addDevice(0x80000000, memory);
    }

    @Test
    public void testLUI() throws MemoryAccessException {
        loadProgram("lui a0, 0xf000");

        board.step(2);
        final R5CPUStateSnapshot state = board.getCpu().getState();
        Assertions.assertEquals(0xf000, state.x[10]);
    }

    @Test
    public void testAUIPC() throws MemoryAccessException {
        loadProgram("auipc a0, 0");

        board.step(2);
        final R5CPUStateSnapshot state = board.getCpu().getState();
        Assertions.assertEquals(state.pc - 4, state.x[10]);
    }

    @Test
    public void testJAL() throws MemoryAccessException {
        loadProgram("jal a0, 0xf0");

        final R5CPUStateSnapshot state1 = board.getCpu().getState();
        board.step(2);
        final R5CPUStateSnapshot state2 = board.getCpu().getState();
        Assertions.assertEquals(state2.pc, state1.pc + 0xf0);
        Assertions.assertEquals(state1.pc + 4, state2.x[10]);

        board.getCpu().setState(state1);
        loadProgram("jal a0, 0xf1");

        board.step(2);
        final R5CPUStateSnapshot state3 = board.getCpu().getState();
        Assertions.assertEquals(state3.pc, state1.pc + 0xf0);
        Assertions.assertEquals(state1.pc + 4, state3.x[10]);
    }

    @Test
    public void testJALR() throws MemoryAccessException {
        loadProgram("addi a0, a0, 0x100",
                "jalr a0, a0, 0x200");

        final R5CPUStateSnapshot state1 = board.getCpu().getState();
        board.step(3);
        final R5CPUStateSnapshot state2 = board.getCpu().getState();
        Assertions.assertEquals(state2.pc, 0x300);
        Assertions.assertEquals(state1.pc + 8, state2.x[10]);
    }

    @Test
    public void testADDI() throws MemoryAccessException {
        loadProgram("addi a0, a0, 42");

        board.step(2);
        final R5CPUStateSnapshot state = board.getCpu().getState();
        Assertions.assertEquals(42, state.x[10]);
    }

    @Test
    public void testBios() throws Exception {
//        final String firmware = "C:\\Users\\fnuecke\\Documents\\Repositories\\Circuity-1.15\\buildroot\\fw_jump.bin";
//        loadProgramFile(firmware, 0);
//
//        final String kernel = "C:\\Users\\fnuecke\\Documents\\Repositories\\Circuity-1.15\\buildroot\\Image";
//        loadProgramFile(kernel, 0x400000);
//
//        final UARTReader reader = new UARTReader(board);
//        final Thread thread = new Thread(reader);
//        thread.start();
//
//        final int n = 10000000;
//        for (int i = 0; i < n; i++) {
//            board.step(100000);
//        }
//
//        reader.stop();
//        thread.join();
    }

    private void loadProgram(final String... value) throws MemoryAccessException {
        R5Assembler.assemble(value, memory, 0);
    }

    private void loadProgramFile(final String path, int address) throws Exception {
        try (final FileInputStream is = new FileInputStream(path)) {
            final BufferedInputStream bis = new BufferedInputStream(is);
            for (int value = bis.read(); value != -1; value = bis.read()) {
                memory.store8(address++, (byte) value);
            }
        }
    }

    private static final class UARTReader implements Runnable {
        private final R5Board board;
        private boolean keepRunning = true;

        private UARTReader(final R5Board board) {
            this.board = board;
        }

        public void stop() {
            keepRunning = false;
        }

        @Override
        public void run() {
            try {
                while (keepRunning) {
                    final int i = board.readValue();
                    if (i >= 0) {
                        System.out.print((char) i);
                    }
                    Thread.yield();
                }
            } catch (final Throwable t) {
                LOGGER.error(t);
            }
        }
    }
}

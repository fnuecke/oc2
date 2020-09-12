package li.cil.circuity;

import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.vm.device.memory.ByteBufferMemory;
import li.cil.circuity.vm.riscv.R5Board;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public final class Main {
    public static void main(final String[] args) throws Exception {
        final PhysicalMemory rom = new ByteBufferMemory(128 * 1024);
        final PhysicalMemory memory = new ByteBufferMemory(48 * 1014 * 1024);
        final R5Board board = new R5Board();
        board.addDevice(0x80000000, rom);
        board.addDevice(0x80000000 + 0x400000, memory);

        final String firmware = "C:\\Users\\fnuecke\\Documents\\Repositories\\Circuity-1.15\\buildroot\\fw_jump.bin";
        loadProgramFile(rom, 0, firmware);

        final String kernel = "C:\\Users\\fnuecke\\Documents\\Repositories\\Circuity-1.15\\buildroot\\Image";
        loadProgramFile(memory, 0, kernel);

        final UARTReader reader = new UARTReader(board);
        final Thread thread = new Thread(reader);
        thread.start();

//        System.out.println("Waiting for VisualVM...");
//        Thread.sleep(10 * 1000);
//        System.out.println("Starting!");

        final long start = System.currentTimeMillis();

        final int n = 40000;
        for (int i = 0; i < n; i++) {
            board.step(10000);
        }

        final long duration = System.currentTimeMillis() - start;

        System.out.printf("Took: %.2fs\n", duration / 1000.0);

        reader.stop();
        thread.join();
    }

    private static void loadProgramFile(final PhysicalMemory memory, int address, final String path) throws Exception {
        try (final FileInputStream is = new FileInputStream(path)) {
            final BufferedInputStream bis = new BufferedInputStream(is);
            for (int value = bis.read(); value != -1; value = bis.read()) {
                memory.store(address++, (byte) value, 0);
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
                t.printStackTrace();
            }
        }
    }
}

package li.cil.circuity;

import li.cil.circuity.api.vm.device.memory.PhysicalMemory;
import li.cil.circuity.api.vm.device.memory.Sizes;
import li.cil.circuity.vm.device.memory.UnsafeMemory;
import li.cil.circuity.vm.riscv.R5Board;
import li.cil.circuity.vm.riscv.R5CPU;

import java.io.BufferedInputStream;
import java.io.FileInputStream;

public final class Main {
    public static void main(final String[] args) throws Exception {
        final PhysicalMemory rom = new UnsafeMemory(128 * 1024);
        final PhysicalMemory memory = new UnsafeMemory(128 * 1014 * 1024);
        final R5Board board = new R5Board();
        board.addDevice(0x80000000, rom);
        board.addDevice(0x80000000 + 0x400000, memory);

        final String firmware = "C:\\Users\\fnuecke\\Documents\\Repositories\\Circuity-1.15\\buildroot\\fw_jump.bin";
        final String kernel = "C:\\Users\\fnuecke\\Documents\\Repositories\\Circuity-1.15\\buildroot\\Image";

        final UARTReader reader = new UARTReader(board);
        final Thread thread = new Thread(reader);
        thread.start();

        System.out.println("Waiting for profiler...");
        Thread.sleep(5 * 1000);
        System.out.println("Starting!");

        final long cyclesPerRun = 300_000_000;
        final int cyclesPerStep = 10_000;
        final int hz = 50_000_000;

        final int samples = 10;
        int minRunDuration = Integer.MAX_VALUE;
        int maxRunDuration = Integer.MIN_VALUE;
        int accRunDuration = 0;

        final R5CPU cpu = board.getCpu();

        for (int i = 0; i < 10; i++) {
            loadProgramFile(memory, 0, kernel);
            loadProgramFile(rom, 0, firmware);
            cpu.reset();

            final long runStart = System.currentTimeMillis();

            int remaining = 0;
            while (cpu.getTime() < cyclesPerRun) {
                final long stepStart = System.currentTimeMillis();

                remaining += hz;
                while (remaining > 0) {
                    board.step(cyclesPerStep);
                    remaining -= cyclesPerStep;
                }

                final long stepDuration = System.currentTimeMillis() - stepStart;
                final long sleep = 1000 - stepDuration;
                if (sleep > 0) {
//                Thread.sleep(sleep);
                } else {
                    System.out.println("Running behind by " + (-sleep) + "ms...");
                }
            }

            final int runDuration = (int) (System.currentTimeMillis() - runStart);
            accRunDuration += runDuration;
            minRunDuration = Integer.min(minRunDuration, runDuration);
            maxRunDuration = Integer.max(maxRunDuration, runDuration);

            System.out.printf("\n\ntime: %.2fs\n", runDuration / 1000.0);
        }

        reader.stop();

        final int avgDuration = accRunDuration / samples;
        System.out.printf("\n\ntimes: min=%.2fs, max=%.2fs, avg=%.2fs\n",
                minRunDuration / 1000.0, maxRunDuration / 1000.0, avgDuration / 1000.0);

        thread.join();
    }

    private static void loadProgramFile(final PhysicalMemory memory, int address, final String path) throws Exception {
        try (final FileInputStream is = new FileInputStream(path)) {
            final BufferedInputStream bis = new BufferedInputStream(is);
            for (int value = bis.read(); value != -1; value = bis.read()) {
                memory.store(address++, (byte) value, Sizes.SIZE_8_LOG2);
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
                final StringBuilder sb = new StringBuilder();
                while (keepRunning) {
                    final int i = board.readValue();
                    if (i >= 0) {
                        final char ch = (char) i;
                        sb.append(ch);
                        if (ch == '\r' || ch == '\n') {
                            final String line = sb.toString();
                            System.out.print(line);
                            sb.setLength(0);
                        }
                    }
                    Thread.yield();
                }
            } catch (final Throwable t) {
                t.printStackTrace();
            }
        }
    }
}

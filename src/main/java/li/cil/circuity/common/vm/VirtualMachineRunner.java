package li.cil.circuity.common.vm;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.circuity.vm.riscv.R5Board;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class VirtualMachineRunner implements Runnable {
    private static final int TIMESLICE_IN_MS = 1000 / 20;

    private static final ExecutorService VM_RUNNERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final R5Board vm;
    private final AtomicInteger timeQuotaInMillis = new AtomicInteger();
    private Future<?> lastSchedule;

    private final ByteArrayFIFOQueue output = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(1024);

    // Thread-local buffers for faster read/writes in inner loop.
    private final ByteArrayFIFOQueue outputBuffer = new ByteArrayFIFOQueue(1024);
    private final ByteArrayFIFOQueue inputBuffer = new ByteArrayFIFOQueue(1024);

    public VirtualMachineRunner(final R5Board vm) {
        this.vm = vm;
    }

    public void tick() {
        if (timeQuotaInMillis.addAndGet(TIMESLICE_IN_MS) > 0 && lastSchedule == null || lastSchedule.isDone() || lastSchedule.isCancelled()) {
            lastSchedule = VM_RUNNERS.submit(this);
        }
    }

    public int readByte() {
        synchronized (output) {
            if (output.isEmpty()) {
                return -1;
            } else {
                return output.dequeueByte();
            }
        }
    }

    public void putByte(final byte value) {
        synchronized (input) {
            input.enqueue(value);
        }
    }

    @Override
    public void run() {
        final long start = System.currentTimeMillis();

        final int cycleBudget = vm.getCpu().getFrequency() / 20;
        final int cyclesPerStep = 1_000;
        final int maxSteps = cycleBudget / cyclesPerStep;

        moveInputToThread();

        for (int i = 0; i < maxSteps; i++) {
            vm.step(cyclesPerStep);
            processVirtualMachineOutput();
            processVirtualMachineInput();

            if (System.currentTimeMillis() - start > timeQuotaInMillis.get()) {
                break;
            }
        }

        moveOutputToMain();

        final int elapsed = (int) (System.currentTimeMillis() - start);
        timeQuotaInMillis.addAndGet(-elapsed);
    }

    private void moveInputToThread() {
        synchronized (input) {
            while (!input.isEmpty()) {
                inputBuffer.enqueue(input.dequeueByte());
            }
        }
    }

    private void moveOutputToMain() {
        synchronized (output) {
            while (!outputBuffer.isEmpty()) {
                output.enqueue(outputBuffer.dequeueByte());
            }
        }
    }

    private void processVirtualMachineOutput() {
        int value;
        while ((value = vm.readValue()) != -1) {
            outputBuffer.enqueue((byte) value);
        }
    }

    private void processVirtualMachineInput() {
        while (!inputBuffer.isEmpty() && vm.canPutValue()) {
            vm.putValue(inputBuffer.dequeueByte());
        }
    }
}

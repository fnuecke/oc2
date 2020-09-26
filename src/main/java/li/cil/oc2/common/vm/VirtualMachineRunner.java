package li.cil.oc2.common.vm;

import li.cil.sedna.riscv.R5Board;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class VirtualMachineRunner implements Runnable {
    private static final int TIMESLICE_IN_MS = 1000 / 20;

    private static final ExecutorService VM_RUNNERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private final R5Board board;
    private final AtomicInteger timeQuotaInMillis = new AtomicInteger();
    private Future<?> lastSchedule;

    public VirtualMachineRunner(final R5Board board) {
        this.board = board;
    }

    public void tick() {
        if (timeQuotaInMillis.addAndGet(TIMESLICE_IN_MS) > 0 && lastSchedule == null || lastSchedule.isDone() || lastSchedule.isCancelled()) {
            lastSchedule = VM_RUNNERS.submit(this);
        }
    }

    protected void handleBeforeRun() {
    }

    protected void step() {
    }

    protected void handleAfterRun() {
    }

    public void join() throws Throwable {
        if (lastSchedule != null) {
            try {
                lastSchedule.get();
            } catch (final InterruptedException e) {
                // We do not mind this.
            } catch (final ExecutionException e) {
                throw e.getCause();
            }
        }
    }

    @Override
    public void run() {
        final long start = System.currentTimeMillis();

        final int cycleBudget = board.getCpu().getFrequency() / 20;
        final int cyclesPerStep = 1_000;
        final int maxSteps = cycleBudget / cyclesPerStep;

        handleBeforeRun();

        for (int i = 0; i < maxSteps; i++) {
            board.step(cyclesPerStep);
            step();

            if (System.currentTimeMillis() - start > timeQuotaInMillis.get()) {
                break;
            }
        }

        handleAfterRun();

        final int elapsed = (int) (System.currentTimeMillis() - start);
        timeQuotaInMillis.addAndGet(-elapsed);
    }
}

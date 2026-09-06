/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMPausingEvent;
import li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent;
import li.cil.oc2.common.Constants;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class VMRunner implements Runnable {
    private static final Logger LOGGER = LogManager.getLogger();

    private static final int TICKS_PER_SECOND = 20;
    private static final int TIMESLICE_IN_MS = 500 / TICKS_PER_SECOND;

    private static final ExecutorService VM_RUNNERS = Executors.newFixedThreadPool(
        Math.max(1, Runtime.getRuntime().availableProcessors()), WorkerThread::new);

    // --------------------------------------------------------------------- //

    private final AbstractArchitecture architecture;
    private final AtomicInteger timeQuotaInMillis = new AtomicInteger();
    private Future<?> lastSchedule;

    // --------------------------------------------------------------------- //

    private boolean firedResumedRunningEvent;
    @Serialized
    private boolean firedInitializationEvent;
    @Serialized
    private volatile Component runtimeError;

    @Serialized
    private long cycleLimit;
    @Serialized
    private long cycles;

    // --------------------------------------------------------------------- //

    public VMRunner(final AbstractArchitecture architecture) {
        this.architecture = architecture;
    }

    // --------------------------------------------------------------------- //

    @Nullable
    public Component getRuntimeError() {
        return runtimeError;
    }

    public void tick() {
        architecture.tickDeviceLayer();

        cycleLimit += getCyclesPerTick();

        final int timeQuota = timeQuotaInMillis.updateAndGet(x -> Math.min(x + TIMESLICE_IN_MS, TIMESLICE_IN_MS));
        final boolean needsScheduling = lastSchedule == null || lastSchedule.isDone() || lastSchedule.isCancelled();
        if (cycleLimit > 0 && timeQuota > 0 && needsScheduling) {
            lastSchedule = VM_RUNNERS.submit(this);
        }
    }

    public void join() {
        if (Thread.currentThread() instanceof WorkerThread) {
            throw new IllegalStateException("Cannot join a virtual machine from a virtual machine worker thread.");
        }

        architecture.sendLifecycleEvent(new VMPausingEvent());
        if (lastSchedule != null) {
            // We have to make sure our worker is joined before passing on an interrupt,
            // to make sure resources the worker uses aren't released from underneath them.
            boolean interrupted = false;
            for (; ; ) {
                try {
                    lastSchedule.get();
                    break;
                } catch (final InterruptedException e) {
                    interrupted = true;
                } catch (final ExecutionException e) {
                    handleRunException(e.getCause());
                    break;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }

        firedResumedRunningEvent = false;
    }

    @Override
    public void run() {
        try {
            runUntilBudgetExhausted();
        } catch (final Throwable e) {
            handleRunException(e);
        }
    }

    // --------------------------------------------------------------------- //

    protected void handleBeforeRun() {
        if (!firedInitializationEvent) {
            firedInitializationEvent = true;
            try {
                architecture.sendInitializingEvent();
            } catch (final VMInitializationException e) {
                architecture.setRunning(false);
                runtimeError = e.getErrorMessage().orElse(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            }
        }

        if (!firedResumedRunningEvent) {
            firedResumedRunningEvent = true;
            architecture.sendLifecycleEvent(new VMResumedRunningEvent());
        }
    }

    protected void step(final int cyclesPerStep) {
        architecture.step(cyclesPerStep);
    }

    protected void handleAfterRun() {
    }

    // --------------------------------------------------------------------- //

    private void runUntilBudgetExhausted() {
        do {
            final long start = System.currentTimeMillis();

            final int cycleBudget = getCyclesPerTick();
            final int cyclesPerStep = 1_000;

            // We run a good number of cycles each tick. We also want a time based limit, but
            // checking that each cycle would be an insane amount of overhead; so we break up
            // the loop into batches and check the time between each batch. We don't just want
            // to bump the cycle per step count, because we want low RPC latency/high througput.
            int steps = cycleBudget / cyclesPerStep;
            final int batchStep = Math.clamp(steps, 1, 100);

            handleBeforeRun();

            if (!architecture.isRunning()) {
                break;
            }

            while (steps > 0) {
                for (int i = 0; i < batchStep && steps > 0; ++i, --steps) {
                    cycles += cyclesPerStep;
                    step(cyclesPerStep);
                }

                if (System.currentTimeMillis() - start > timeQuotaInMillis.get()) {
                    break;
                }
            }

            handleAfterRun();

            final int elapsed = (int) (System.currentTimeMillis() - start);
            timeQuotaInMillis.addAndGet(-elapsed);
        } while (cycles < cycleLimit && timeQuotaInMillis.get() > 0);
    }

    private void handleRunException(final Throwable e) {
        LOGGER.error("Virtual machine failed while running.", e);
        architecture.setRunning(false);
        runtimeError = Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN);
    }

    private int getCyclesPerTick() {
        return architecture.getFrequency() / TICKS_PER_SECOND;
    }

    // --------------------------------------------------------------------- //

    private static final class WorkerThread extends Thread {
        private WorkerThread(final Runnable runnable) {
            super(runnable, "OC2 VM Runner");
            setDaemon(true);
        }
    }
}

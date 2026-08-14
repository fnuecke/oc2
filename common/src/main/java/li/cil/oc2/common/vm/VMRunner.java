/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.api.bus.device.vm.event.VMPausingEvent;
import li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public class VMRunner implements Runnable {
    private static final int TICKS_PER_SECOND = 20;
    private static final int TIMESLICE_IN_MS = 500 / TICKS_PER_SECOND;

    private static final ExecutorService VM_RUNNERS = Executors.newCachedThreadPool(r -> {
        final Thread thread = new Thread(r);
        thread.setDaemon(true);
        thread.setName("VirtualMachine Runner");
        return thread;
    });

    // ------------------------------------------------------------- //

    private final R5Board board;
    private final GlobalVMContext context;
    private final RPCDeviceBusAdapter rpcAdapter;
    private final AtomicInteger timeQuotaInMillis = new AtomicInteger();
    private Future<?> lastSchedule;

    // ------------------------------------------------------------- //

    private boolean firedResumedRunningEvent;
    @Serialized
    private boolean firedInitializationEvent;
    @Serialized
    private Component runtimeError;

    @Serialized
    private long cycleLimit;
    @Serialized
    private long cycles;

    // ------------------------------------------------------------- //

    public VMRunner(final AbstractVirtualMachine virtualMachine) {
        this.board = virtualMachine.state.board;
        context = virtualMachine.state.context;
        rpcAdapter = virtualMachine.state.rpcAdapter;
    }

    // ------------------------------------------------------------- //

    @Nullable
    public Component getRuntimeError() {
        return runtimeError;
    }

    public void tick() {
        rpcAdapter.tick();

        cycleLimit += getCyclesPerTick();

        final int timeQuota = timeQuotaInMillis.updateAndGet(x -> Math.min(x + TIMESLICE_IN_MS, TIMESLICE_IN_MS));
        final boolean needsScheduling = lastSchedule == null || lastSchedule.isDone() || lastSchedule.isCancelled();
        if (cycleLimit > 0 && timeQuota > 0 && needsScheduling) {
            lastSchedule = VM_RUNNERS.submit(this);
        }
    }

    public void join() {
        context.postEvent(new VMPausingEvent());
        firedResumedRunningEvent = false;
        if (lastSchedule != null) {
            try {
                lastSchedule.get();
            } catch (final InterruptedException e) {
                // We do not mind this.
            } catch (final ExecutionException e) {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public void run() {
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

            if (!board.isRunning()) {
                break;
            }

            while (steps > 0) {
                for (int i = 0; i < batchStep && steps > 0; ++i, --steps) {
                    cycles += cyclesPerStep;
                    board.step(cyclesPerStep);
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

    // ------------------------------------------------------------- //

    protected void handleBeforeRun() {
        if (!firedInitializationEvent) {
            firedInitializationEvent = true;
            try {
                context.postEvent(new VMInitializingEvent(board.getDefaultProgramStart()));
            } catch (final VMInitializationException e) {
                board.setRunning(false);
                runtimeError = e.getErrorMessage().orElse(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            }
        }

        if (!firedResumedRunningEvent) {
            firedResumedRunningEvent = true;
            context.postEvent(new VMResumedRunningEvent());
        }
    }

    protected void step(final int cyclesPerStep) {
        rpcAdapter.step(cyclesPerStep);
    }

    protected void handleAfterRun() {
    }

    // ------------------------------------------------------------- //

    private static int getCyclesPerTick() {
        return Constants.CPU_FREQUENCY / TICKS_PER_SECOND;
    }
}

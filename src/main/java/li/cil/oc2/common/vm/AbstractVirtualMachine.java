/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.bus.RPCDeviceBusAdapter;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TickUtils;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.riscv.R5Board;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;

public abstract class AbstractVirtualMachine implements VirtualMachine {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    private static final String STATE_TAG_NAME = "state";
    private static final String RUNNER_TAG_NAME = "runner";

    public static final String BUS_STATE_TAG_NAME = "busState";
    public static final String RUN_STATE_TAG_NAME = "runState";
    public static final String BOOT_ERROR_TAG_NAME = "bootError";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));

    ///////////////////////////////////////////////////////////////////

    public final CommonDeviceBusController busController;
    private CommonDeviceBusController.BusState busState = CommonDeviceBusController.BusState.SCAN_PENDING;
    private int loadDevicesDelay;

    @Serialized
    public static final class SerializedState {
        public R5Board board;
        public GlobalVMContext context;
        public BuiltinDevices builtinDevices;
        public RPCDeviceBusAdapter rpcAdapter;
        public transient VMDeviceBusAdapter vmAdapter;
    }

    public final SerializedState state = new SerializedState();
    public AbstractTerminalVMRunner runner;
    private VMRunState runState = VMRunState.STOPPED;
    @Nullable private Component bootError;

    ///////////////////////////////////////////////////////////////////

    public AbstractVirtualMachine(final CommonDeviceBusController busController) {
        this.busController = busController;

        busController.onBeforeDeviceScan.add(this::handleBeforeDeviceScan);
        busController.onAfterDeviceScan.add(this::handleAfterDeviceScan);
        busController.onDevicesAdded.add(this::handleDevicesAdded);
        busController.onDevicesRemoved.add(this::handleDevicesRemoved);

        state.board = new R5Board();
        state.context = new GlobalVMContext(state.board);
        state.builtinDevices = new BuiltinDevices(state.context);
        state.rpcAdapter = new RPCDeviceBusAdapter(state.builtinDevices.rpcSerialDevice);
        state.vmAdapter = new VMDeviceBusAdapter(state.context);

        state.board.getCpu().setFrequency(Constants.CPU_FREQUENCY);
        state.board.setBootArguments("root=/dev/vda rw");
        state.board.setStandardOutputDevice(state.builtinDevices.uart);
    }

    ///////////////////////////////////////////////////////////////////

    public void dispose() {
        joinWorkerThread();
        state.context.invalidate();
        busController.dispose();
    }

    public void suspend() {
        joinWorkerThread();
        state.vmAdapter.unmountDevices();
        state.rpcAdapter.unmountDevices();
    }

    @Override
    public boolean isRunning() {
        return getBusState() == CommonDeviceBusController.BusState.READY &&
            getRunState() == VMRunState.RUNNING;
    }

    @Override
    public CommonDeviceBusController.BusState getBusState() {
        return busState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBusStateClient(final CommonDeviceBusController.BusState value) {
        busState = value;
    }

    @Override
    public VMRunState getRunState() {
        return runState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setRunStateClient(final VMRunState value) {
        runState = value;
    }

    @Override
    @Nullable
    public Component getBootError() {
        return bootError;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBootErrorClient(@Nullable final Component value) {
        bootError = value;
    }

    @Override
    @Nullable
    public Component getError() {
        switch (busState) {
            case SCAN_PENDING:
            case INCOMPLETE:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_INCOMPLETE);
            case TOO_COMPLEX:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_TOO_COMPLEX);
            case MULTIPLE_CONTROLLERS:
                return Component.translatable(Constants.COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS);
            case READY:
                switch (runState) {
                    case STOPPED:
                    case LOADING_DEVICES:
                        return bootError;
                }
                break;
        }
        return null;
    }

    @Override
    public void start() {
        if (runState == VMRunState.RUNNING) {
            return;
        }

        setBootError(null);
        setRunState(VMRunState.LOADING_DEVICES);
        loadDevicesDelay = 0;
    }

    @Override
    public void stop() {
        stopRunnerAndReset();
    }

    public void tick() {
        busController.scan();
        setBusState(busController.getState());
        if (busState != CommonDeviceBusController.BusState.READY) {
            return;
        }

        if (state.board.isRestarting()) {
            stop();
            start();
        }

        switch (runState) {
            case LOADING_DEVICES -> load();
            case RUNNING -> run();
        }
    }

    public CompoundTag serialize() {
        joinWorkerThread();

        final CompoundTag tag = new CompoundTag();

        if (runner != null) {
            tag.put(RUNNER_TAG_NAME, NBTSerialization.serialize(runner));
        } else {
            NBTUtils.putEnum(tag, RUN_STATE_TAG_NAME, runState);
        }

        tag.put(STATE_TAG_NAME, NBTSerialization.serialize(state));

        return tag;
    }

    public void deserialize(final CompoundTag tag) {
        joinWorkerThread();

        if (tag.contains(RUNNER_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            runner = createRunner();
            NBTSerialization.deserialize(tag.getCompound(RUNNER_TAG_NAME), runner);
            runState = VMRunState.LOADING_DEVICES;
        } else {
            runState = NBTUtils.getEnum(tag, RUN_STATE_TAG_NAME, VMRunState.class);
            if (runState == null) {
                runState = VMRunState.STOPPED;
            } else if (runState == VMRunState.RUNNING) {
                runState = VMRunState.LOADING_DEVICES;
            }
        }

        if (tag.contains(STATE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(tag.getCompound(STATE_TAG_NAME), state);
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract AbstractTerminalVMRunner createRunner();

    protected abstract boolean consumeEnergy(final int amount, final boolean simulate);

    protected void handleBusStateChanged(final CommonDeviceBusController.BusState value) {
    }

    protected void handleRunStateChanged(final VMRunState value) {
    }

    protected void handleBootErrorChanged(@Nullable final Component value) {
    }

    protected void error(@Nullable final Component message) {
        error(message, true);
    }

    protected void error(@Nullable final Component message, final boolean reset) {
        if (reset) {
            stopRunnerAndReset();
        }
        setBootError(message);
    }

    protected void stopRunnerAndReset() {
        joinWorkerThread();
        setRunState(VMRunState.STOPPED);

        state.board.setRunning(false);
        state.board.reset();
        state.rpcAdapter.reset();
        state.rpcAdapter.disposeDevices();
        state.vmAdapter.disposeDevices();

        runner = null;
    }

    ///////////////////////////////////////////////////////////////////

    private void joinWorkerThread() {
        if (runner != null) {
            runner.join();
        }
    }

    private void load() {
        if (loadDevicesDelay > 0) {
            loadDevicesDelay--;
            return;
        }

        if (!consumeEnergy(busController.getEnergyConsumption(), true)) {
            // Don't even start running if we couldn't keep running.
            error(Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY));
            return;
        }

        if (busController.getDevices().stream().noneMatch(device -> device instanceof FirmwareLoader)) {
            error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_FIRMWARE));
            return;
        }

        assert runner == null : "Runner active while still in load phase.";

        final VMDeviceLoadResult loadResult = state.vmAdapter.mountDevices();
        if (!loadResult.wasSuccessful()) {
            if (loadResult.getErrorMessage() != null) {
                error(loadResult.getErrorMessage(), false);
            } else {
                error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN), false);
            }
            loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
            return;
        }

        // May have a valid runner after load. In which case we just had to wait for
        // bus setup and devices to load. So we can keep using it.
        if (runner == null) {
            try {
                state.board.reset();
                state.board.initialize();
                state.board.setRunning(true);
            } catch (final IllegalStateException e) {
                // FDT did not fit into memory. Technically it's possible to run with
                // a program that only uses registers. But not supporting that esoteric
                // use-case loses out against avoiding people getting confused for having
                // forgotten to add some RAM modules.
                error(Component.translatable(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
                return;
            } catch (final MemoryAccessException e) {
                LOGGER.error(e);
                error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            }

            runner = createRunner();
        }

        state.rpcAdapter.mountDevices();

        setRunState(VMRunState.RUNNING);

        // Only start running next tick. Doing so gives loaded devices one tick to do async
        // initialization. This is used by devices to restore data from disk, for example.
    }

    private void run() {
        final Component runtimeError = runner.getRuntimeError();
        if (runtimeError != null) {
            error(runtimeError);
            return;
        }

        if (!state.board.isRunning()) {
            stopRunnerAndReset();
            return;
        }

        if (!consumeEnergy(busController.getEnergyConsumption(), false)) {
            error(Component.translatable(Constants.COMPUTER_ERROR_NOT_ENOUGH_ENERGY));
            return;
        }

        runner.tick();
    }

    private void setBusState(final CommonDeviceBusController.BusState value) {
        if (value == busState) {
            return;
        }

        busState = value;

        handleBusStateChanged(busState);
    }

    private void setRunState(final VMRunState value) {
        if (value == runState) {
            return;
        }

        runState = value;

        handleRunStateChanged(value);
    }

    private void setBootError(@Nullable final Component value) {
        bootError = value;
        handleBootErrorChanged(value);
    }

    private void handleBeforeDeviceScan() {
        state.rpcAdapter.pause();

        // Since scans can be delayed we must adjust our run state accordingly, to avoid
        // running before the scan finishes.
        if (runState == VMRunState.RUNNING) {
            runState = VMRunState.LOADING_DEVICES;
        }
    }

    private void handleAfterDeviceScan(final CommonDeviceBusController.AfterDeviceScanEvent event) {
        state.rpcAdapter.resume(busController, event.didDevicesChange());
    }

    private void handleDevicesAdded(final CommonDeviceBusController.DevicesChangedEvent event) {
        joinWorkerThread();
        state.vmAdapter.addDevices(event.devices());
    }

    private void handleDevicesRemoved(final CommonDeviceBusController.DevicesChangedEvent event) {
        joinWorkerThread();
        state.vmAdapter.removeDevices(event.devices());
    }
}

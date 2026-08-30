/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.oc2.common.util.TickUtils;
import li.cil.sedna.api.memory.MemoryAccessException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.time.Duration;
import java.util.function.LongSupplier;

public abstract class AbstractVirtualMachine implements VirtualMachine, VirtualMachineClientState {
    private static final Logger LOGGER = LogManager.getLogger();

    // --------------------------------------------------------------------- //

    private static final String ARCHITECTURE_TAG_NAME = "architecture";
    private static final String STATE_TAG_NAME = "state";
    private static final String RUNNER_TAG_NAME = "runner";

    public static final String BUS_STATE_TAG_NAME = "busState";
    public static final String RUN_STATE_TAG_NAME = "runState";
    public static final String BOOT_ERROR_TAG_NAME = "bootError";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = TickUtils.toTicks(Duration.ofSeconds(10));

    // --------------------------------------------------------------------- //

    private final CommonDeviceBusController busController;
    private CommonDeviceBusController.BusState busState = CommonDeviceBusController.BusState.SCAN_PENDING;
    private int loadDevicesDelay;

    @Nullable
    private AbstractArchitecture architecture;
    @Nullable
    private PendingState pending;

    private DeviceLocationProvider deviceLocationProvider = unused -> DeviceLocation.UNSPECIFIED;
    private LongSupplier gameTimeSource = () -> 0L;

    private AbstractTerminalVMRunner runner;
    private VMRunState runState = VMRunState.STOPPED;
    @Nullable
    private Component bootError;

    // --------------------------------------------------------------------- //

    public AbstractVirtualMachine(final CommonDeviceBusController busController) {
        this.busController = busController;

        busController.onBeforeDeviceScan.add(this::handleBeforeDeviceScan);
        busController.onAfterDeviceScan.add(this::handleAfterDeviceScan);
        busController.onDevicesAdded.add(this::handleDevicesAdded);
        busController.onDevicesRemoved.add(this::handleDevicesRemoved);
    }

    // --------------------------------------------------------------------- //

    public CommonDeviceBusController getBusController() {
        return busController;
    }

    public void setGameTimeSource(final LongSupplier gameTime) {
        gameTimeSource = gameTime;
    }

    public boolean sendEvent(final String type, @Nullable final Object data) {
        return architecture != null && architecture.sendGuestEvent(type, data);
    }

    public void dispose() {
        disposeArchitecture();
        busController.dispose();
    }

    public void suspend() {
        joinWorkerThread();
        if (architecture != null) {
            architecture.unmountDevices();
        }
    }

    // --------------------------------------------------------------------- //
    // VirtualMachine

    @Override
    public CommonDeviceBusController.BusState getBusState() {
        return busState;
    }

    @Override
    public VMRunState getRunState() {
        return runState;
    }

    @Override
    @Nullable
    public Component getBootError() {
        return bootError;
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
    public boolean isRunning() {
        return getBusState() == CommonDeviceBusController.BusState.READY &&
            getRunState() == VMRunState.RUNNING;
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

    // --------------------------------------------------------------------- /
    // VirtualMachineClientState

    @Override
    @Environment(EnvType.CLIENT)
    public void setBusStateClient(final CommonDeviceBusController.BusState value) {
        busState = value;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void setRunStateClient(final VMRunState value) {
        runState = value;
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void setBootErrorClient(@Nullable final Component value) {
        bootError = value;
    }

    // --------------------------------------------------------------------- //

    public void tick() {
        busController.scan();
        setBusState(busController.getState());
        if (busState != CommonDeviceBusController.BusState.READY) {
            return;
        }

        if (architecture != null && architecture.isRestarting()) {
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

        if (architecture == null) {
            // Didn't get to set architecture, save what we loaded, if anything.
            if (pending != null) {
                tag.putString(ARCHITECTURE_TAG_NAME, pending.architectureType().name());
                tag.put(STATE_TAG_NAME, pending.state());
            }
            if (pending != null && pending.runner() != null) {
                tag.put(RUNNER_TAG_NAME, pending.runner());
            } else {
                NBTUtils.putEnum(tag, RUN_STATE_TAG_NAME, runState);
            }
            return tag;
        }

        if (runner != null) {
            tag.put(RUNNER_TAG_NAME, NBTSerialization.serialize(runner));
        } else {
            NBTUtils.putEnum(tag, RUN_STATE_TAG_NAME, runState);
        }

        tag.put(STATE_TAG_NAME, NBTSerialization.serialize(architecture));
        tag.putString(ARCHITECTURE_TAG_NAME, architecture.getType().name());

        return tag;
    }

    public void deserialize(final CompoundTag tag) {
        joinWorkerThread();

        final CompoundTag runnerTag = tag.contains(RUNNER_TAG_NAME, NBTTagIds.TAG_COMPOUND)
            ? tag.getCompound(RUNNER_TAG_NAME) : null;
        if (runnerTag != null) {
            runState = VMRunState.LOADING_DEVICES;
        } else {
            runState = NBTUtils.getEnum(tag, RUN_STATE_TAG_NAME, VMRunState.class);
            if (runState == null) {
                runState = VMRunState.STOPPED;
            } else if (runState == VMRunState.RUNNING) {
                runState = VMRunState.LOADING_DEVICES;
            }
        }

        final ArchitectureType architectureType = findArchitecture(tag.getString(ARCHITECTURE_TAG_NAME));
        pending = architectureType != null && tag.contains(STATE_TAG_NAME, NBTTagIds.TAG_COMPOUND)
            ? new PendingState(architectureType, tag.getCompound(STATE_TAG_NAME), runnerTag)
            : null;
    }

    public void joinWorkerThread() {
        if (runner != null) {
            runner.join();
        }
    }

    // --------------------------------------------------------------------- //

    protected final void setDeviceLocationProvider(final DeviceLocationProvider provider) {
        deviceLocationProvider = provider;
    }

    protected abstract AbstractTerminalVMRunner createRunner(AbstractArchitecture architecture);

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

        if (architecture != null) {
            architecture.stopAndReset();
        }

        pending = null;
        runner = null;
    }

    // --------------------------------------------------------------------- //

    // Technically private, for tests only.
    public long getInstructionsRetired() {
        return architecture != null ? architecture.getInstructionsRetired() : 0;
    }

    @Nullable
    private static ArchitectureType findArchitecture(final String name) {
        for (final ArchitectureType value : ArchitectureType.values()) {
            if (value.name().equals(name)) {
                return value;
            }
        }

        LOGGER.warn("Discarding state of unknown architectureType [{}].", name);
        return null;
    }

    private void applyArchitecture(final ArchitectureType type) {
        if (architecture != null && architecture.getType() == type) {
            applyPendingState(architecture);
            return;
        }

        joinWorkerThread();

        if (architecture != null) {
            architecture.stopAndReset();
            architecture.dispose();
            runner = null;
        }

        architecture = switch (type) {
            case RISCV -> new R5Architecture();
            case Z80 -> new Z80Architecture();
        };

        architecture.setDeviceLocationProvider(device -> deviceLocationProvider.getDeviceLocation(device));
        architecture.setGameTimeSource(() -> gameTimeSource.getAsLong());

        applyPendingState(architecture);

        architecture.addDevices(busController.getDevices());
        architecture.handleAfterDeviceScan(busController, true);
    }

    private void disposeArchitecture() {
        joinWorkerThread();
        if (architecture != null) {
            architecture.dispose();
            architecture = null;
            runner = null;
        }
    }

    private void applyPendingState(final AbstractArchitecture architecture) {
        final PendingState pending = this.pending;
        this.pending = null;

        if (pending == null || pending.architectureType() != architecture.getType()) {
            return;
        }

        try {
            NBTSerialization.deserialize(pending.state(), architecture);
            if (pending.runner() != null) {
                runner = createRunner(architecture);
                NBTSerialization.deserialize(pending.runner(), runner);
            }
        } catch (final Throwable e) {
            LOGGER.error("Failed restoring virtual machine state; it will start cold.", e);
            setRunState(VMRunState.STOPPED);
            runner = null;
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

        final var selected = busController.getArchitectureType();
        if (selected.isEmpty()) {
            error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_CPU));
            return;
        }

        applyArchitecture(selected.get());

        if (busController.getDevices().stream().noneMatch(device -> device instanceof FirmwareLoader)) {
            error(Component.translatable(Constants.COMPUTER_ERROR_MISSING_FIRMWARE));
            return;
        }

        final VMDeviceLoadResult loadResult = architecture.mountDevices();
        if (!loadResult.wasSuccessful()) {
            final Component message = loadResult.getErrorMessage() != null
                ? loadResult.getErrorMessage()
                : Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN);
            error(message, loadResult.isPermanent());
            if (!loadResult.isPermanent()) {
                loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
            }
            return;
        }

        // May have a valid runner after load. In which case we just had to wait for
        // bus setup and devices to load. So we can keep using it.
        if (runner == null) {
            try {
                architecture.boot();
            } catch (final IllegalStateException e) {
                // FDT did not fit into memory. Technically it's possible to run with
                // a program that only uses registers. But not supporting that esoteric
                // use-case loses out against avoiding people getting confused for having
                // forgotten to add some RAM modules.
                error(Component.translatable(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
                return;
            } catch (final MemoryAccessException e) {
                LOGGER.error("Failed booting virtual machine.", e);
                error(Component.translatable(Constants.COMPUTER_ERROR_UNKNOWN));
                return;
            }

            runner = createRunner(architecture);
        }

        architecture.startDevicesLayer();

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

        if (!architecture.isRunning()) {
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
        if (architecture != null) {
            architecture.handleBeforeDeviceScan();
        }

        // Since scans can be delayed we must adjust our run state accordingly, to avoid
        // running before the scan finishes.
        if (runState == VMRunState.RUNNING) {
            runState = VMRunState.LOADING_DEVICES;
        }
    }

    private void handleAfterDeviceScan(final CommonDeviceBusController.AfterDeviceScanEvent event) {
        if (architecture != null) {
            architecture.handleAfterDeviceScan(busController, event.didDevicesChange());
        }
    }

    private void handleDevicesAdded(final CommonDeviceBusController.DevicesChangedEvent event) {
        joinWorkerThread();
        if (architecture != null) {
            architecture.addDevices(event.devices());
        }
    }

    private void handleDevicesRemoved(final CommonDeviceBusController.DevicesChangedEvent event) {
        joinWorkerThread();
        if (architecture != null) {
            architecture.removeDevices(event.devices());
        }
    }

    private record PendingState(ArchitectureType architectureType, CompoundTag state, @Nullable CompoundTag runner) {
    }
}

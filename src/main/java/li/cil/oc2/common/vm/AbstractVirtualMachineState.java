package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.VMDeviceLifecycleEventType;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.util.NBTTagIds;
import li.cil.oc2.common.util.NBTUtils;
import li.cil.sedna.api.memory.MemoryAccessException;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.util.Objects;

public abstract class AbstractVirtualMachineState<TBusController extends AbstractDeviceBusController, TVirtualMachine extends VirtualMachine> implements VirtualMachineState {
    private static final Logger LOGGER = LogManager.getLogger();

    ///////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////

    private static final String VIRTUAL_MACHINE_TAG_NAME = "virtualMachine";
    private static final String RUNNER_TAG_NAME = "runner";

    public static final String BUS_STATE_TAG_NAME = "busState";
    public static final String RUN_STATE_TAG_NAME = "runState";
    public static final String BOOT_ERROR_TAG_NAME = "bootError";

    private static final int DEVICE_LOAD_RETRY_INTERVAL = 10 * Constants.TICK_SECONDS;

    ///////////////////////////////////////////////////////////////////

    public final TBusController busController;
    private AbstractDeviceBusController.BusState busState = AbstractDeviceBusController.BusState.SCAN_PENDING;

    public final TVirtualMachine virtualMachine;
    private AbstractTerminalVirtualMachineRunner runner;
    private RunState runState = RunState.STOPPED;
    private ITextComponent bootError;
    private int loadDevicesDelay;

    ///////////////////////////////////////////////////////////////////

    public AbstractVirtualMachineState(final TBusController busController, final TVirtualMachine virtualMachine) {
        this.busController = busController;
        this.virtualMachine = virtualMachine;

    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean isRunning() {
        return getBusState() == AbstractDeviceBusController.BusState.READY &&
               getRunState() == RunState.RUNNING;
    }

    @Override
    public AbstractDeviceBusController.BusState getBusState() {
        return busState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setBusStateClient(final AbstractDeviceBusController.BusState value) {
        busState = value;
    }

    @Override
    public RunState getRunState() {
        return runState;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setRunStateClient(final RunState value) {
        runState = value;
    }

    @Override
    @Nullable
    public ITextComponent getBootError() {
        switch (busState) {
            case SCAN_PENDING:
            case INCOMPLETE:
                return new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_INCOMPLETE);
            case TOO_COMPLEX:
                return new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_TOO_COMPLEX);
            case MULTIPLE_CONTROLLERS:
                return new TranslationTextComponent(Constants.COMPUTER_BUS_STATE_MULTIPLE_CONTROLLERS);
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
    @OnlyIn(Dist.CLIENT)
    public void setBootErrorClient(final ITextComponent value) {
        bootError = value;
    }

    @Override
    public void start() {
        if (runState == RunState.RUNNING) {
            return;
        }

        setBootError(null);
        setRunState(RunState.LOADING_DEVICES);
        loadDevicesDelay = 0;
    }

    @Override
    public void stop() {
        switch (runState) {
            case LOADING_DEVICES:
                setRunState(RunState.STOPPED);
                break;
            case RUNNING:
                stopRunnerAndReset();
                break;
        }
    }

    public void reload() {
        if (runState == RunState.RUNNING) {
            runState = RunState.LOADING_DEVICES;
        }
    }

    public void joinVirtualMachine() {
        if (runner != null) {
            try {
                runner.join();
            } catch (final Throwable e) {
                LOGGER.error(e);
                runner = null;
            }
        }
    }

    public void stopRunnerAndReset() {
        joinVirtualMachine();
        setRunState(RunState.STOPPED);

        virtualMachine.reset();

        if (runner != null) {
            runner.resetTerminal();
            runner = null;
        }
    }

    public void tick() {
        busController.scan();
        setBusState(busController.getState());
        if (busState != AbstractDeviceBusController.BusState.READY) {
            return;
        }

        switch (runState) {
            case STOPPED:
                break;
            case LOADING_DEVICES:
                if (loadDevicesDelay > 0) {
                    loadDevicesDelay--;
                    break;
                }

                final VMDeviceLoadResult loadResult = virtualMachine.vmAdapter.load();
                if (!loadResult.wasSuccessful()) {
                    if (loadResult.getErrorMessage() != null) {
                        setBootError(loadResult.getErrorMessage());
                    } else {
                        setBootError(new TranslationTextComponent(Constants.COMPUTER_BOOT_ERROR_UNKNOWN));
                    }
                    loadDevicesDelay = DEVICE_LOAD_RETRY_INTERVAL;
                    break;
                }

                // May have a valid runner after load. In which case we just had to wait for
                // bus setup and devices to load. So we can keep using it.
                if (runner == null) {
                    try {
                        virtualMachine.board.reset();
                        virtualMachine.board.initialize();
                        virtualMachine.board.setRunning(true);
                    } catch (final IllegalStateException e) {
                        // FDT did not fit into memory. Technically it's possible to run with
                        // a program that only uses registers. But not supporting that esoteric
                        // use-case loses out against avoiding people getting confused for having
                        // forgotten to add some RAM modules.
                        setBootError(new TranslationTextComponent(Constants.COMPUTER_BOOT_ERROR_NO_MEMORY));
                        setRunState(RunState.STOPPED);
                        return;
                    } catch (final MemoryAccessException e) {
                        LOGGER.error(e);
                        setBootError(new TranslationTextComponent(Constants.COMPUTER_BOOT_ERROR_UNKNOWN));
                        setRunState(RunState.STOPPED);
                        return;
                    }

                    runner = createRunner();
                }

                setRunState(RunState.RUNNING);

                // Only start running next tick. This gives loaded devices one tick to do async
                // initialization. This is used by devices to restore data from disk, for example.
                break;
            case RUNNING:
                if (!virtualMachine.board.isRunning()) {
                    stopRunnerAndReset();
                    break;
                }

                runner.tick();
                break;
        }
    }

    public CompoundNBT serialize() {
        final CompoundNBT tag = new CompoundNBT();

        joinVirtualMachine();

        if (runner != null) {
            tag.put(RUNNER_TAG_NAME, NBTSerialization.serialize(runner));
            virtualMachine.vmAdapter.fireLifecycleEvent(VMDeviceLifecycleEventType.PAUSING);
            runner.scheduleResumeEvent(); // Allow synchronizing to async device saves.
        } else {
            NBTUtils.putEnum(tag, RUN_STATE_TAG_NAME, runState);
        }
        tag.put(VIRTUAL_MACHINE_TAG_NAME, NBTSerialization.serialize(virtualMachine));

        return tag;
    }

    public void deserialize(final CompoundNBT tag) {
        joinVirtualMachine();

        if (tag.contains(RUNNER_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            runner = createRunner();
            NBTSerialization.deserialize(tag.getCompound(RUNNER_TAG_NAME), runner);
            runState = RunState.LOADING_DEVICES;
        } else {
            runState = NBTUtils.getEnum(tag, RUN_STATE_TAG_NAME, RunState.class);
            if (runState == null) {
                runState = RunState.STOPPED;
            } else if (runState == RunState.RUNNING) {
                runState = RunState.LOADING_DEVICES;
            }
        }

        if (tag.contains(VIRTUAL_MACHINE_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
            NBTSerialization.deserialize(tag.getCompound(VIRTUAL_MACHINE_TAG_NAME), virtualMachine);
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract AbstractTerminalVirtualMachineRunner createRunner();

    protected void handleBusStateChanged(final AbstractDeviceBusController.BusState value) {
    }

    protected void handleRunStateChanged(final RunState value) {
    }

    protected void handleBootErrorChanged(@Nullable final ITextComponent value) {
    }

    ///////////////////////////////////////////////////////////////////

    private void setBusState(final AbstractDeviceBusController.BusState value) {
        if (value == busState) {
            return;
        }

        busState = value;

        handleBusStateChanged(busState);
    }

    private void setRunState(final RunState value) {
        if (value == runState) {
            return;
        }

        runState = value;

        handleRunStateChanged(value);
    }

    private void setBootError(@Nullable final ITextComponent value) {
        if (Objects.equals(value, bootError)) {
            return;
        }

        bootError = value;

        handleBootErrorChanged(value);
    }
}

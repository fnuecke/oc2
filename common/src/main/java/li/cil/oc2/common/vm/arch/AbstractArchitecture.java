/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.arch;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMRuntime;
import li.cil.oc2.common.vm.DeviceLocation;
import li.cil.oc2.common.vm.DeviceLocationProvider;
import li.cil.oc2.common.vm.VMDeviceRegistry;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.DeviceBus;
import li.cil.sedna.api.device.rtc.RealTimeCounter;
import li.cil.sedna.api.device.serial.SerialDevice;
import li.cil.sedna.api.memory.MemoryAccessException;

import javax.annotation.Nullable;
import java.util.OptionalLong;
import java.util.Set;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;

public abstract class AbstractArchitecture {
    @Serialized
    private final GlobalVMContext context;
    private final transient VMDeviceRegistry vmRegistry;
    private transient Set<Device> devices = Set.of();

    public record Config(
        DeviceLocationProvider deviceLocationProvider,
        VMRuntime runtime,
        LongSupplier gameTimeProvider,
        IntPredicate consumeEnergy) {
    }

    // --------------------------------------------------------------------- //

    protected AbstractArchitecture(final Board board, final RealTimeCounter clock, final Config config) {
        this(board, clock, config, null);
    }

    protected AbstractArchitecture(
        final Board board,
        final RealTimeCounter clock,
        final Config config,
        @Nullable final DeviceBus deviceBus
    ) {
        context = new GlobalVMContext(board, clock, config.runtime(), deviceBus);
        vmRegistry = new VMDeviceRegistry(context, device -> getDeviceAddress(config.deviceLocationProvider().getDeviceLocation(device)));
    }

    // --------------------------------------------------------------------- //
    // Configuration / State

    public abstract ArchitectureType getType();

    public abstract int getFrequency();

    public abstract SerialDevice getTerminalDevice();

    public abstract long getInstructionsRetired();

    protected OptionalLong getDeviceAddress(final DeviceLocation location) {
        return OptionalLong.empty();
    }

    // --------------------------------------------------------------------- //
    // Runtime

    public abstract void boot() throws MemoryAccessException;

    public abstract void step(int cycles);

    public abstract boolean isRunning();

    public abstract void setRunning(boolean value);

    public boolean isRestarting() {
        return false;
    }

    public abstract void reset();

    public final void stopAndReset() {
        setRunning(false);
        reset();
        resetDynamicDevices();
        unmountAllDevices();
        devices.forEach(Device::dispose);
    }

    public abstract void sendInitializingEvent();

    public final void sendLifecycleEvent(final Object event) {
        context.sendEvent(event);
    }

    // --------------------------------------------------------------------- //
    // General device bus API / lifecycle

    public VMDeviceLoadResult mountVMDevices() {
        return vmRegistry.mountDevices();
    }

    public void unmountAllDevices() {
        vmRegistry.unmountDevices();
    }

    public void dispose() {
        context.invalidate();
    }

    // --------------------------------------------------------------------- //
    // High level device bus API

    public void handleAfterDeviceScan(final DeviceBusController controller) {
        devices = Set.copyOf(controller.getDevices());
        vmRegistry.rebuild(controller);
    }

    public void mountDynamicDevices() {
    }

    public void tickDynamicDevices() {
    }

    protected void resetDynamicDevices() {
    }

    // --------------------------------------------------------------------- //

    protected final GlobalVMContext getContext() {
        return context;
    }
}

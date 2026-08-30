/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.common.vm.context.global.GlobalVMContext;
import li.cil.sedna.api.Board;
import li.cil.sedna.api.DeviceBus;
import li.cil.sedna.api.device.serial.SerialDevice;
import li.cil.sedna.api.memory.MemoryAccessException;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.OptionalLong;
import java.util.function.LongSupplier;

public abstract class AbstractArchitecture {
    @Serialized
    private final GlobalVMContext context;
    private final transient VMDeviceBusAdapter vmAdapter;

    // --------------------------------------------------------------------- //

    protected AbstractArchitecture(final Board board) {
        this(board, null);
    }

    protected AbstractArchitecture(final Board board, @Nullable final DeviceBus deviceBus) {
        context = new GlobalVMContext(board, deviceBus);
        vmAdapter = new VMDeviceBusAdapter(context);
    }

    // --------------------------------------------------------------------- //
    // Configuration / State

    public final void setDeviceLocationProvider(final DeviceLocationProvider provider) {
        vmAdapter.setBaseAddressProvider(device -> getDeviceAddress(provider.getDeviceLocation(device)));
    }

    public void setGameTimeSource(final LongSupplier gameTime) {
    }

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
        resetDeviceLayer();
        disposeDevices();
    }

    public abstract void sendInitializingEvent();

    public final void sendLifecycleEvent(final Object event) {
        context.sendEvent(event);
    }

    public boolean sendGuestEvent(final String type, @Nullable final Object data) {
        return false;
    }

    // --------------------------------------------------------------------- //
    // General device bus API / lifecycle

    public void addDevices(final Collection<Device> devices) {
        vmAdapter.addDevices(devices);
    }

    public void removeDevices(final Collection<Device> devices) {
        vmAdapter.removeDevices(devices);
    }

    public VMDeviceLoadResult mountDevices() {
        return vmAdapter.mountDevices();
    }

    public void unmountDevices() {
        vmAdapter.unmountDevices();
    }

    public void disposeDevices() {
        vmAdapter.disposeDevices();
    }

    public void dispose() {
        context.invalidate();
    }

    // --------------------------------------------------------------------- //
    // High level device bus API

    public void handleBeforeDeviceScan() {
    }

    public void handleAfterDeviceScan(DeviceBusController controller, boolean didDevicesChange) {
    }

    public void startDevicesLayer() {
    }

    public void tickDeviceLayer() {
    }

    protected void resetDeviceLayer() {
    }

    // --------------------------------------------------------------------- //

    protected final GlobalVMContext getContext() {
        return context;
    }
}

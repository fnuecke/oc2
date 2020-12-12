package li.cil.oc2.api.bus.device.vm;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;

/**
 * Allows adding {@link MemoryMappedDevice}s directly to the underlying
 * virtual machine.
 * <p>
 * This is a more low-level approach than the {@link RPCDevice}. Devices
 * implemented through this interface will require explicit driver support
 * in the guest system.
 */
public interface VMDevice extends Device {
    /**
     * Called to initialize this device.
     * <p>
     * Register {@link MemoryMappedDevice}s and claim interrupts via the
     * {@link InterruptAllocator} made available through the {@code context}.
     * <p>
     * If loading cannot complete, e.g. because resources cannot be allocated,
     * this should return {@code false}. The virtual machine will periodically
     * try again to load failed devices. The virtual machine will only start
     * running after all devices have successfully loaded.
     *
     * @param context the virtual machine context.
     * @return {@code true} if the device was loaded successfully; {@code false} otherwise.
     */
    VMDeviceLoadResult load(VMContext context);

    /**
     * Called when the device is unloaded.
     * <p>
     * This is guaranteed to be called when the device is disposed, be it because
     * it was removed from the {@link DeviceBus} or because of the virtual machine
     * being destroyed.
     * <p>
     * Release unmanaged resources here.
     */
    void unload();
}

package li.cil.oc2.api.bus.device.vm;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.sedna.api.device.MemoryMappedDevice;

/**
 * Allows adding {@link MemoryMappedDevice}s directly to the underlying
 * virtual machine.
 * <p>
 * This is a more low-level approach than the {@link RPCDevice}. Devices
 * implemented through this interface will require explicit driver support
 * in the guest system.
 * <p>
 * To listen to lifecycle events of the VM and the device, implement the
 * {@link VMDeviceLifecycleListener} interface. This is particularly useful
 * for releasing unmanaged resources acquired in {@link #load(VMContext)}.
 *
 * @see VMDeviceLifecycleListener
 * @see li.cil.oc2.api.bus.device.provider.BlockDeviceProvider
 * @see li.cil.oc2.api.bus.device.provider.ItemDeviceProvider
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
}

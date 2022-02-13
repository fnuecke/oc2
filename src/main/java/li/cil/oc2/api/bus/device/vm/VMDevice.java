/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.vm.context.InterruptAllocator;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.sedna.api.device.MemoryMappedDevice;

/**
 * Allows adding {@link MemoryMappedDevice}s directly to the underlying
 * virtual machine.
 * <p>
 * This is a more low-level approach than the {@link RPCDevice}. Devices
 * implemented through this interface will require explicit driver support
 * in the guest system.
 * <p>
 * To listen to lifecycle events of the VM and the device, register to the event
 * bus provided via {@link VMContext#getEventBus()} in {@link #mount(VMContext)}.
 * <p>
 * The lifecycle for VMDevices can be depicted as such:
 * <pre>
 * ┌──────────────┐ ┌────────────────┐
 * │serializeNBT()│ │deserializeNBT()◄───────┐
 * └──────────────┘ └───────┬────────┘       │
 *   May be called          │VM starts or    │
 *   at any time,    ┌──────┤resumes after   │
 *   except while    │      │load            │
 *   unloaded...     │  ┌───▼───┐            │
 *                   │  │mount()│            │
 *                   │  └───┬───┘            │Chunk
 *                   │      │VM stops or     │unloaded
 *                   │      │is unloaded     │
 *                   │      │                │
 *                   │ ┌────▼────┐           │
 *                   │ │unmount()├───────────┤
 *                   │ └────┬────┘           │
 *                   │      │VM stopped or   │
 *                   │      │device removed  │
 *                   │      │                │
 *                   │ ┌────▼────┐           │
 *                   └─┤dispose()├───────────┘
 *                     └─────────┘
 * </pre>
 * Note that if any other {@link VMDevice} fails mounting, all mounted devices
 * will immediately unmounted and disposed.
 *
 * @see li.cil.oc2.api.bus.device.provider.BlockDeviceProvider
 * @see li.cil.oc2.api.bus.device.provider.ItemDeviceProvider
 */
public interface VMDevice extends Device {
    /**
     * Called to start this device.
     * <p>
     * This is called when the connected virtual machine starts, or when the device
     * is added to a {@link DeviceBus} with a currently running virtual machine.
     * <p>
     * Register {@link MemoryMappedDevice}s and claim interrupts via the
     * {@link InterruptAllocator} made available through the {@code context}.
     * <p>
     * If loading cannot complete, e.g. because resources cannot be allocated,
     * this should return {@code false}. The virtual machine will periodically
     * try again to load failed devices. The virtual machine will only start
     * or resume after all devices have successfully loaded.
     *
     * @param context the virtual machine context.
     * @return {@code true} if the device was loaded successfully; {@code false} otherwise.
     */
    VMDeviceLoadResult mount(VMContext context);

    /**
     * Called to pause this device.
     * <p>
     * Called when the connected virtual machine is suspended (chunk unload/server stopped/...).
     * <p>
     * Also called when the connected virtual machine stops or the device is removed from a
     * {@link DeviceBus} with a currently running virtual machine. In this case, {@link #dispose()}
     * will be called after this method returns.
     */
    void unmount();
}

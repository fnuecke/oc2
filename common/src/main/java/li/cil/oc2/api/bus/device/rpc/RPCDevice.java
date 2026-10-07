/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;

import java.util.List;

/**
 * Provides an interface for an RPC device, describing the methods that can be
 * called on it and the type name it can be detected by/is compatible with.
 * <p>
 * One underlying device may have multiple {@link RPCDevice}s, providing different methods for the
 * device. This allows specifying general purpose interfaces, which provide logic
 * for some aspect of an underlying device, which may be shared with other devices.
 * <p>
 * The easiest, and hence recommended, way of implementing this interface, is to use
 * the {@link ObjectDevice} class, which collects {@link li.cil.oc2.api.bus.device.object.Callback} methods.
 * <p>
 * The lifecycle for {@link RPCDevice}s is as follows:
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
 *
 * @see ObjectDevice
 * @see li.cil.oc2.api.bus.device.object.Callback
 * @see li.cil.oc2.api.bus.device.provider.BlockDeviceProvider
 * @see li.cil.oc2.api.bus.device.provider.ItemDeviceProvider
 * @see li.cil.oc2.api.bus.device.io.IODevice
 */
public interface RPCDevice extends Device {
    /**
     * The type name identifying this interface.
     * <p>
     * Guests find devices by type name. All {@link RPCDevice}s of one underlying device
     * are presented to the guest as one device, which can be found by any of their type names.
     *
     * @return the type name.
     */
    String getTypeName();

    /**
     * The list of methods groups provided by this interface.
     *
     * @return the list of method groups.
     */
    List<RPCMethodGroup> getMethodGroups();

    /**
     * Called to start this device.
     * <p>
     * This is called when the connected virtual machine starts, or when the device
     * is added to a {@link DeviceBus} with a currently running virtual machine.
     *
     * @param context the device's handle on the computer it was mounted in.
     */
    default void mount(final RPCBusContext context) {
    }

    /**
     * Called to pause this device.
     * <p>
     * Called when the connected virtual machine is suspended (chunk unload/server stopped/...).
     * <p>
     * Also called when the connected virtual machine stops or the device is removed from a
     * {@link DeviceBus} with a currently running virtual machine. In this case, {@link #dispose()}
     * will be called after this method returns.
     * <p>
     * If {@link #mount(RPCBusContext)} was called, this is guaranteed to be called.
     *
     * @param context the context passed to the matching {@link #mount(RPCBusContext)}. No longer valid.
     */
    default void unmount(final RPCBusContext context) {
    }
}

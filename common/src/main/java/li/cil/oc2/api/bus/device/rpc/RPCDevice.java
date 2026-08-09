/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.rpc;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;

import java.util.List;

/**
 * Provides an interface for an RPC device, describing the methods that can be
 * called on it and the type names it can be detected by/is compatible with.
 * <p>
 * A {@link RPCDevice} may represent a single view onto some device, or be a
 * collection of multiple aggregated {@link RPCDevice}s. One underlying device
 * may have multiple {@link RPCDevice}s, providing different methods for the
 * device. This allows specifying general purpose interfaces, which provide logic
 * for some aspect of an underlying device, which may be shared with other devices.
 * <p>
 * The easiest, and hence recommended, way of implementing this interface, is to use
 * the {@link ObjectDevice} class.
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
 * @see li.cil.oc2.api.bus.device.provider.BlockDeviceProvider
 * @see li.cil.oc2.api.bus.device.provider.ItemDeviceProvider
 */
public interface RPCDevice extends Device {
    /**
     * A list of type names identifying this interface.
     * <p>
     * Device interfaces may be identified by multiple type names. Although every
     * atomic implementation will usually only have one, when compounding interfaces
     * all the underlying type names can thus be retained.
     * <p>
     * In a more general sense, these can be considered tags the device can be
     * referenced by inside a VM.
     *
     * @return the list of type names.
     */
    List<String> getTypeNames();

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
     */
    default void mount() {
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
     * If {@link #mount()} was called, this is guaranteed to be called.
     */
    default void unmount() {
    }
}

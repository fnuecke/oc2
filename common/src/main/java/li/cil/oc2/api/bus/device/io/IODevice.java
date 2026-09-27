/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.api.bus.device.object.IODeviceDescription;
import li.cil.oc2.api.bus.device.object.ObjectDevice;

import java.util.List;

/**
 * Provides an interface for an IO device, describing the methods that can be
 * called on it and the name it can be detected by.
 * <p>
 * The easiest, and hence recommended, way of implementing this interface is to use the
 * {@link ObjectDevice} class, which collects {@link IOCallback} methods.
 *
 * @see ObjectDevice
 * @see IOCallback
 * @see li.cil.oc2.api.bus.device.rpc.RPCDevice
 */
public interface IODevice {
    /**
     * The name this device is known by to the guest.
     *
     * @return the guest-visible name.
     * @see IODeviceDescription
     */
    String getIOName();

    /**
     * The list of methods provided by this device.
     *
     * @return the list of methods.
     */
    List<IOMethod> getIOMethods();

    /**
     * Called when the device is made available to a computer's mid-level API.
     * <p>
     * The context stays valid until {@link #unmountIO(IOBusContext)} is called with it.
     *
     * @param context the context the device may send events through.
     */
    default void mountIO(final IOBusContext context) {
    }

    /**
     * Called when the device is no longer available to a computer's mid-level API.
     *
     * @param context the context passed to {@link #mountIO(IOBusContext)}.
     */
    default void unmountIO(final IOBusContext context) {
    }
}

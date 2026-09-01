/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.io;

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
     * @see IOName
     */
    String getIOName();

    /**
     * The list of methods provided by this device.
     *
     * @return the list of methods.
     */
    List<IOMethod> getIOMethods();
}

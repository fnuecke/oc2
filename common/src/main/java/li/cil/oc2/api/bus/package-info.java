/* SPDX-License-Identifier: MIT */

/**
 * The device bus is the glue that connects devices and VMs.
 * <p>
 * A bus must always be managed by a {@link li.cil.oc2.api.bus.DeviceBusController}.
 * If there is no controller, there is no (connected) bus.
 * <p>
 * When a controller performs a scan, it collects a list of connected
 * {@link li.cil.oc2.api.bus.DeviceBusElement}s thus defining a
 * {@link li.cil.oc2.api.bus.DeviceBus}.
 * How the controller scans for elements depends on the implementation.
 * One example is a block-based controller which scans adjacent blocks
 * in a recursive manner -- usually up to some maximum bus complexity.
 * <p>
 * {@link li.cil.oc2.api.bus.DeviceBusElement}s are responsible for
 * providing a list of devices connected to them. Whether they play an
 * active role and seek out devices, or passively expect devices to be
 * registered with them depends on the implementation.
 * <p>
 * After a scan {@link li.cil.oc2.api.bus.DeviceBusController}s then
 * collect all devices from all bus elements to build a global set
 * of devices on the bus.
 * <p>
 * There can be various types of devices on a bus, but which types are
 * supported will depend on the context of the controller. Currently, two
 * types of devices are defined in this API, {@link li.cil.oc2.api.bus.device.rpc.RPCDevice}
 * and {@link li.cil.oc2.api.bus.device.vm.VMDevice}.
 * <ul>
 * <li>
 * RPC devices are a high-level system for providing VMs with means of
 * calling methods on such devices. The protocol used allows VMs to
 * dynamically detect what devices are present and what methods may be
 * called on them.
 * </li>
 * <li>
 * VM devices are low-level devices that may register emulations of actual
 * devices with a VM. Such devices will require drivers to be present
 * inside the VM to work.
 * </li>
 * </ul>
 */
@ParametersAreNonnullByDefault
@MethodsReturnNonnullByDefault
package li.cil.oc2.api.bus;

import net.minecraft.MethodsReturnNonnullByDefault;

import javax.annotation.ParametersAreNonnullByDefault;

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;

import java.util.Optional;

/**
 * This is used to query for devices given a block face.
 * <p>
 * Implementations <em>may</em> return various device types depending on the query.
 * <p>
 * For identical queries and world state, implementations <em>should</em> return the same device.
 * Failing that, implementations <em>should</em> return instances that are equal to each other
 * when compared using {@link Object#equals(Object)} and have equal {@link Object#hashCode()}s.
 * <p>
 * This allows avoiding unnecessary re-initialization of devices that have not changed since a
 * previous scan.
 * <p>
 * This is also required to avoid device duplication when a device is connected to a
 * {@link li.cil.oc2.api.bus.DeviceBus} more than once. An example where this can occur are
 * blocks that expose the same device on all sides having connected cabling adjacent to more
 * than one face.
 * <p>
 * Providers are registered with the {@link li.cil.oc2.api.util.Registries#BLOCK_DEVICE_PROVIDER}
 * registry, much like blocks and items. See {@code docs/api.md} for the loader-specific details.
 *
 * @see li.cil.oc2.api.bus.device.rpc.RPCDevice
 * @see li.cil.oc2.api.bus.device.object.ObjectDevice
 * @see li.cil.oc2.api.bus.device.vm.VMDevice
 * @see BlockDeviceQuery
 */
public interface BlockDeviceProvider {
    /**
     * Get a device for the specified query.
     * <p>
     * Buses query again on neighbor updates and when capabilities at the queried position are
     * invalidated. A block whose devices change without either should invalidate its capabilities
     * (NeoForge: {@code invalidateCapabilities()}) or, on Fabric, notify its neighbors
     * ({@code Level#updateNeighborsAt}).
     *
     * @param query the query describing the object to get a {@link Device} for.
     * @return a device for the specified query, if available.
     */
    Optional<Device> getDevice(BlockDeviceQuery query);
}

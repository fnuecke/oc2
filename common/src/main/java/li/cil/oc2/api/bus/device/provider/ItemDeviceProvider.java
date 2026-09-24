/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.ItemDevice;

import java.util.Optional;

/**
 * This is used to query for devices given an item stack.
 * <p>
 * Implementations <em>may</em> return various device types depending on the query.
 * <p>
 * For identical queries, implementations <em>should</em> return the same device. Failing that,
 * implementations <em>should</em> return instances that are equal to each other when compared
 * using {@link Object#equals(Object)} and have equal {@link Object#hashCode()}s.
 * <p>
 * This allows avoiding unnecessary re-initialization of devices that have not changed since a
 * previous scan.
 * <p>
 * Providers are registered with the {@link li.cil.oc2.api.util.Registries#ITEM_DEVICE_PROVIDER}
 * registry, much like blocks and items. See {@code docs/api.md} for the loader-specific details.
 *
 * @see li.cil.oc2.api.bus.device.rpc.RPCDevice
 * @see li.cil.oc2.api.bus.device.object.ObjectDevice
 * @see li.cil.oc2.api.bus.device.vm.VMDevice
 * @see ItemDeviceQuery
 */
public interface ItemDeviceProvider {
    /**
     * Get a device for the specified query.
     *
     * @param query the query describing the object to get an {@link ItemDevice} for.
     * @return a device for the specified query, if available.
     */
    Optional<ItemDevice> getDevice(ItemDeviceQuery query);

    /**
     * The amount of energy the device that would be returned by {@link #getDevice(ItemDeviceQuery)}
     * will consume per tick while the VM using it is running.
     * <p>
     * Return <code>0</code> if no device would be provided.
     *
     * @param query the query describing the object to get an {@link ItemDevice} for.
     * @return the amount of energy consumed by the device each tick.
     */
    default int getEnergyConsumption(final ItemDeviceQuery query) {
        return 0;
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.util.Invalidatable;
import net.minecraft.nbt.CompoundTag;

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
 * Providers can be registered via the provider registries, much like blocks and items
 * are registered. For example:
 * <pre>
 * class YourModInitialization {
 *     static DeferredRegister&lt;BlockDeviceProvider&gt; BLOCK_DEVICE_PROVIDERS = DeferredRegister.create(BlockDeviceProvider.REGISTRY, "your_mod_id");
 *
 *     static void initialize() {
 *         BLOCK_DEVICE_PROVIDERS.register("your_block_device_name", YourBlockDeviceProvider::new);
 *
 *         BLOCK_DEVICE_PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *     }
 * }
 * </pre>
 *
 * @see li.cil.oc2.api.bus.device.rpc.RPCDevice
 * @see li.cil.oc2.api.bus.device.object.ObjectDevice
 * @see li.cil.oc2.api.bus.device.vm.VMDevice
 * @see BlockDeviceQuery
 */
public interface BlockDeviceProvider {
    /**
     * Get a device for the specified query.
     *
     * @param query the query describing the object to get a {@link Device} for.
     * @return a device for the specified query, if available.
     */
    Invalidatable<Device> getDevice(BlockDeviceQuery query);

    /**
     * Last-resort cleanup method for devices provided by this provider.
     * <p>
     * This is the equivalent of {@link RPCDevice#dispose()} or {@link VMDevice#dispose()},
     * for devices that have gone missing unexpectedly, so this method could no longer be
     * called on the actual device.
     * <p>
     * For block devices, this can happen if the block the device was created for has been
     * removed while the connected computer was unloaded, or the cable connecting the block
     * the device was provided for with the computer was broken while the computer was unloaded.
     * <p>
     * Implementing this is only necessary, if the device holds some out-of-NBT serialized
     * data, or does something similar.
     *
     * @param query the query that resulted in a missing device being detected.
     * @param tag   data last serialized by the device that went missing.
     */
    default void unmount(final BlockDeviceQuery query, final CompoundTag tag) {
    }
}

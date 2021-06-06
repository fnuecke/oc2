package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.ItemDevice;
import net.minecraftforge.registries.IForgeRegistryEntry;

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
 * Providers can be registered via the provider registries, much like blocks and items
 * are registered. For example:
 * <pre>
 * class YourModInitialization {
 *     static DeferredRegister&lt;ItemDeviceProvider&gt; ITEM_DEVICE_PROVIDERS = DeferredRegister.create(ItemDeviceProvider.class, "your_mod_id");
 *
 *     static void initialize() {
 *         ITEM_DEVICE_PROVIDERS.register("your_item_device_name", YourItemDeviceProvider::new);
 *
 *         ITEM_DEVICE_PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *     }
 * }
 * </pre>
 *
 * @see li.cil.oc2.api.bus.device.rpc.RPCDevice
 * @see li.cil.oc2.api.bus.device.object.ObjectDevice
 * @see li.cil.oc2.api.bus.device.vm.VMDevice
 * @see ItemDeviceQuery
 */
public interface ItemDeviceProvider extends IForgeRegistryEntry<ItemDeviceProvider> {
    /**
     * Get a device for the specified query.
     *
     * @param query the query describing the object to get an {@link ItemDevice} for.
     * @return a device for the specified query, if available.
     */
    Optional<ItemDevice> getDevice(ItemDeviceQuery query);

    /**
     * Get the type of a device that would be obtained from {@link #getDevice(ItemDeviceQuery)}
     * if called with the same query. The device type controls which slot devices may be
     * inserted in in item device containers.
     *
     * @param query the query describing the object to get the {@link DeviceType} for.
     * @return the device type for the specified type, if available.
     */
    default Optional<DeviceType> getDeviceType(final ItemDeviceQuery query) {
        return Optional.of(DeviceTypes.CARD);
    }

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

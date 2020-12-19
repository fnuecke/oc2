package li.cil.oc2.api.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistryEntry;

/**
 * Allows querying for devices given some context.
 * <p>
 * See the specializations of {@link DeviceQuery} for possible queries.
 * <ul>
 * <li>Implementations <em>may</em> handle multiple query types and return various device
 * types depending on the query.</li>
 * <li>
 * Implementations <em>should</em> return the same device for the same query.
 * <p>
 * Failing that, implementations <em>should</em> return instances that are equal to each
 * other when compared using {@link Object#equals(Object)} and have equal {@link Object#hashCode()}s.
 * <p>
 * This is required to avoid device duplication when a device is connected to a bus more
 * than once (e.g. for blocks when connected cables are adjacent to multiple faces of the
 * block).
 * </li>
 * </ul>
 * <p>
 * Providers can be registered via the device provider registry, much like blocks and items
 * are registered. For example:
 * <pre>
 * class YourModInitialization {
 *     static DeferredRegister<DeviceProvider> PROVIDERS = DeferredRegister.create(DeviceProvider.class, "your_mod_id");
 *
 *     static void initialize() {
 *         PROVIDERS.register("your_device_name", YourDeviceProvider::new);
 *
 *         PROVIDERS.register(FMLJavaModLoadingContext.get().getModEventBus());
 *     }
 * }
 * </pre>
 *
 * @see RPCDevice
 * @see ObjectDevice
 * @see DeviceQuery
 * @see BlockDeviceQuery
 */
public interface DeviceProvider extends IForgeRegistryEntry<DeviceProvider> {
    /**
     * Get a device for the specified query.
     *
     * @param query the query describing the object to get a {@link Device} for.
     * @return a device for the specified query, if available.
     */
    LazyOptional<Device> getDevice(DeviceQuery query);
}

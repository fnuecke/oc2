package li.cil.oc2.api.device.provider;

import li.cil.oc2.api.device.Device;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Allows querying for devices given some context.
 * <p>
 * See the specializations of {@link DeviceQuery} for possible queries.
 * <p>
 * Returning a device does <em>not</em> transfer ownership of the device in terms of
 * responsibility for persistence. Callers of this method will <em>not</em> attempt
 * to persist devices returned by this method. It is the responsibility of the provider
 * to ensure persistence where required. Typically by returning devices that are
 * themselves persisted objects such as {@link net.minecraft.tileentity.TileEntity}s
 * or storing data in a related persisted object.
 * <p>
 * Implementations <em>may</em> handle multiple query types and return various device
 * types depending on the query.
 * <p>
 * Implementations <em>should</em> return the same device for the same query.
 * <p>
 * Implementations <em>must</em> return the same device type for the same query.
 * <p>
 * Providers can be registered with the IMC message {@link li.cil.oc2.api.API#IMC_ADD_DEVICE_PROVIDER}.
 *
 * @see DeviceQuery
 * @see BlockDeviceQuery
 */
@FunctionalInterface
public interface DeviceProvider {
    /**
     * Get a device for the specified query.
     *
     * @param query the query describing the object to get a {@link Device} for.
     * @return a device for the specified query, if available.
     */
    LazyOptional<Device> getDevice(DeviceQuery query);
}

package li.cil.oc2.api.provider;

import li.cil.oc2.api.bus.device.DeviceInterface;
import li.cil.oc2.api.bus.device.object.ObjectDeviceInterface;
import net.minecraftforge.common.util.LazyOptional;

/**
 * Allows querying for device interfaces given some context.
 * <p>
 * See the specializations of {@link DeviceQuery} for possible queries.
 * <p>
 * Returning a device interface does <em>not</em> transfer ownership of the device in terms
 * of responsibility for persistence. Callers of this method will <em>not</em> attempt
 * to persist objects returned by this method. It is the responsibility of the provider
 * to ensure persistence where required.
 * <ul>
 * <li>Implementations <em>may</em> handle multiple query types and return various device
 * interface types depending on the query.</li>
 * <li>
 * Implementations <em>should</em> return the same device interface for the same query.
 * <p>
 * Failing that, implementations <em>should</em> return instances that are equal to each
 * other when compared using {@link #equals(Object)} and have equal {@link #hashCode()}s.
 * <p>
 * This is required to avoid device duplication when a device is connected to a bus more
 * than once (e.g. for blocks when connected cables are adjacent to multiple faces of the
 * block).
 * </li>
 * </ul>
 * <p>
 * Providers can be registered with the IMC message {@link li.cil.oc2.api.API#IMC_ADD_DEVICE_PROVIDER}.
 *
 * @see DeviceInterface
 * @see ObjectDeviceInterface
 * @see DeviceQuery
 * @see BlockDeviceQuery
 */
@FunctionalInterface
public interface DeviceInterfaceProvider {
    /**
     * Get a device for the specified query.
     *
     * @param query the query describing the object to get a {@link DeviceInterface} for.
     * @return a device for the specified query, if available.
     */
    LazyOptional<DeviceInterface> getDeviceInterface(DeviceQuery query);
}

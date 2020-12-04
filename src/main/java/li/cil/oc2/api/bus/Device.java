package li.cil.oc2.api.bus;

/**
 * Base interface for objects that can be registered as devices on a {@link DeviceBus}.
 * <p>
 * Which types are handled/supported by a bus depends on the {@link DeviceBusController}
 * managing the bus.
 * <p>
 * Note that it is strongly encouraged for implementations to provide an overloaded
 * {@link #equals(Object)} and {@link #hashCode()} so that identical devices can be
 * detected.
 */
public interface Device {
}

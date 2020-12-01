package li.cil.oc2.api.device;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.device.provider.DeviceInterfaceProvider;

import java.util.UUID;

/**
 * Specialization of a device interface that can be referenced by a {@link UUID} and
 * represents a device as a whole.
 * <p>
 * Implementations will typically act as a top-level wrapper for one singular device
 * as viewed by a list of {@link DeviceInterface}s.
 * <p>
 * A unique ID is required when adding devices to a {@link DeviceBus} such that they
 * may then be referenced.
 * <p>
 * Note that {@link DeviceInterfaceProvider}s are <em>not</em> expected to return instances
 * implementing this interface, but return regular {@link DeviceInterface}s instead.
 */
public interface Device extends DeviceInterface {
    /**
     * An id unique to this device.
     * <p>
     * This id must persist over save/load to prevent code in a running VM losing
     * track of the device.
     */
    UUID getUniqueIdentifier();

    /**
     * Returns a possible underlying instance of a device.
     * <p>
     * Frequently some {@link DeviceInterface} obtained from a {@link DeviceInterfaceProvider} will be
     * wrapped by an instance of this interface. To prevent this leading to duplicated
     * device listings this allows accessing the device proper for equality checks.
     *
     * @return the underlying device. May be this device itself.
     */
    default DeviceInterface getIdentifiedDevice() {
        return this;
    }
}

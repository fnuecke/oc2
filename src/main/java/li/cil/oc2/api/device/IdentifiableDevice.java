package li.cil.oc2.api.device;

import li.cil.oc2.api.device.provider.DeviceProvider;

import java.util.UUID;

/**
 * Specialization of devices that allows referencing the device by a {@link UUID}.
 * <p>
 * This type is required when adding devices to a {@link li.cil.oc2.api.bus.DeviceBus}
 * or referencing devices on a bus. Some {@link li.cil.oc2.api.bus.DeviceBusElement}s
 * may take care of wrapping connected devices automatically.
 * <p>
 * Note that {@link DeviceProvider}s are <em>not</em>
 * required to return identifiable devices.
 */
public interface IdentifiableDevice extends Device {
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
     * Frequently some {@link Device} obtained from a {@link DeviceProvider} will be
     * wrapped by an instance of this interface. To prevent this leading to duplicated
     * device listings this allows accessing the device proper for equality checks.
     *
     * @return the underlying device. May be this device itself.
     */
    default Device getIdentifiedDevice() {
        return this;
    }
}

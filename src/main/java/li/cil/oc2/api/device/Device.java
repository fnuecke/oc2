package li.cil.oc2.api.device;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.device.object.ObjectDevice;

import java.util.List;
import java.util.UUID;

/**
 * Defines a device that may be added to a {@link DeviceBus}.
 * <p>
 * The easiest and hence recommended way of implementing this interface is to use
 * the {@link ObjectDevice} class.
 *
 * @see ObjectDevice
 */
public interface Device {
    /**
     * An id unique to this device.
     * <p>
     * This id must persist over save/load to prevent code in a running VM losing
     * track of the device.
     */
    UUID getUniqueId();

    /**
     * A list of device type names for this device.
     * <p>
     * Devices may be identified by multiple type names. Although every atomic
     * implementation will usually only have one, when compounding such modular
     * devices into a {@link CompoundDevice} all the underlying type names can
     * thus be retained.
     * <p>
     * In a more general sense, these can be considered tags the device can be
     * referenced by inside a VM.
     */
    List<String> getTypeNames();

    /**
     * The list of methods implemented by this device.
     */
    List<DeviceMethod> getMethods();
}

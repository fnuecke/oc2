package li.cil.oc2.api.device;

import li.cil.oc2.api.bus.DeviceBus;
import li.cil.oc2.api.device.object.ObjectDevice;
import li.cil.oc2.api.device.provider.DeviceProvider;

import java.util.List;

/**
 * Defines a device that may be added to a {@link DeviceBus}.
 * <p>
 * The easiest and hence recommended way of implementing this interface is to use
 * the {@link ObjectDevice} class.
 * <p>
 * Note that it is strongly encouraged for implementations to provide an overloaded
 * {@link #equals(Object)} and {@link #hashCode()} so that identical devices can be
 * detected.
 *
 * @see ObjectDevice
 * @see DeviceProvider
 */
public interface Device {
    /**
     * A list of device type names for this device.
     * <p>
     * Devices may be identified by multiple type names. Although every atomic
     * implementation will usually only have one, when compounding such modular
     * devices all the underlying type names can thus be retained.
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

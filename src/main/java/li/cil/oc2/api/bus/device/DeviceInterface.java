package li.cil.oc2.api.bus.device;

import li.cil.oc2.api.bus.device.object.ObjectDeviceInterface;
import li.cil.oc2.api.provider.DeviceInterfaceProvider;

import java.util.List;

/**
 * Implementations act as an interface for a device.
 * <p>
 * A {@link DeviceInterface} represents a single view onto some device. One device
 * may have multiple {@code DeviceInterfaces} providing different methods for the
 * device. This allows specifying general purpose interfaces which provide logic
 * for some aspect of an underlying device which may be shared with other devices.
 * <p>
 * The easiest and hence recommended way of implementing this interface is to use
 * the {@link ObjectDeviceInterface} class.
 * <p>
 * Note that it is strongly encouraged for implementations to provide an overloaded
 * {@link #equals(Object)} and {@link #hashCode()} so that identical devices can be
 * detected.
 *
 * @see ObjectDeviceInterface
 * @see DeviceInterfaceProvider
 */
public interface DeviceInterface {
    /**
     * A list of type names identifying this interface.
     * <p>
     * Device interfaces may be identified by multiple type names. Although every
     * atomic implementation will usually only have one, when compounding interfaces
     * all the underlying type names can thus be retained.
     * <p>
     * In a more general sense, these can be considered tags the device can be
     * referenced by inside a VM.
     */
    List<String> getTypeNames();

    /**
     * The list of methods provided by this interface.
     */
    List<DeviceMethod> getMethods();
}

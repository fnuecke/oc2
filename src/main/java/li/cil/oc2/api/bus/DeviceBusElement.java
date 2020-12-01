package li.cil.oc2.api.bus;

import li.cil.oc2.api.device.Device;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;

/**
 * Represents a single connection point on a device bus.
 * <p>
 * The only way for {@link DeviceBusElement}s to be added to a bus is for a
 * {@link DeviceBusController} to detect them during a scan.
 * <p>
 * When discovered during a scan, the controller will then use the devices
 * connected to this element.
 * <p>
 * This interface is only relevant when implementing it, e.g. to provide a custom
 * "cable" or other means to extend a bus.
 */
public interface DeviceBusElement extends DeviceBus {
    /**
     * The controller this bus element is currently registered with, if any.
     *
     * @return the current controller.
     */
    Optional<DeviceBusController> getController();

    /**
     * Sets the controller this bus element is now registered with, if any.
     * <p>
     * This will be called by {@link DeviceBusController}s when scanning.
     *
     * @param controller the new controller.
     */
    void setController(@Nullable final DeviceBusController controller);

    /**
     * Returns the list of devices connected specifically by this element.
     * <p>
     * This differs from {@link #getDevices()} in such that {@link #getDevices()} will
     * return all devices connected to the controller, if this element is registered
     * with a controller.
     * <p>
     * This method is called by the {@link DeviceBusController} the element is registered
     * with when the global list of devices is rebuilt, e.g. after a call to
     * {@link DeviceBusController#scanDevices()}.
     *
     * @return the devices that have been added to this element.
     */
    Collection<Device> getLocalDevices();
}

package li.cil.oc2.api.bus;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

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
     * Sets the {@link DeviceBusController} of the bus this element is on. This effectively
     * adds this bus element to the bus controlled by the specified {@link DeviceBusController}.
     * <p>
     * This will be called by {@link DeviceBusController}s when scanning.
     * <p>
     * When {@link #scheduleScan()} is called, {@link DeviceBusController#scheduleBusScan()}
     * <em>must</em> be called for the current controller.
     * <p>
     * When either {@link #addDevice(Device)} or {@link #removeDevice(Device)} are called,
     * {@link DeviceBusController#scanDevices()} <em>must</em> be called for the registered
     * controller.
     *
     * @param controller the controller to set. {@code null} when being removed from a bus.
     */
    void setController(@Nullable DeviceBusController controller);

    /**
     * Get the bus controller of the bus this element is on, if any.
     *
     * @return the bus controller.
     */
    Optional<DeviceBusController> getController();

    /**
     * Returns a stream of adjacent bus elements.
     * <p>
     * May return {@link Optional#empty()} when the neighbors cannot be
     * determined at this time. This is typically the case when an adjacent
     * block is currently not loaded. Not to be confused with an empty stream,
     * which simply means the element has no neighbors.
     *
     * @return the adjacent bus elements, if possible.
     */
    Optional<Collection<LazyOptional<DeviceBusElement>>> getNeighbors();

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

    /**
     * Returns an identifier unique to the specified device.
     * <p>
     * This id must persist over save/load to prevent code in a running VM losing
     * track of the device. Note that some device types (e.g. {@link RPCDevice}s)
     * require for an ID to be provided for them to work at all.
     * <p>
     * It is possible for multiple devices to have the same identifier. Typically
     * this means they represent a view on the same underlying object. How this is
     * handled depends on the device type and may or may not be supported.
     * <p>
     * Only devices retrieved by calling {@link #getLocalDevices()} should be passed
     * to this method.
     *
     * @param device the device to obtain the ID for.
     * @return the stable id for the specified device.
     */
    Optional<UUID> getDeviceIdentifier(Device device);

    @Override
    default void scheduleScan() {
        getController().ifPresent(DeviceBusController::scheduleBusScan);
    }
}

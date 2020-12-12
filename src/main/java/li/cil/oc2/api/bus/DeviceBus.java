package li.cil.oc2.api.bus;

import java.util.Collection;

/**
 * A device bus provides the interface by which {@link Device}s can be made available
 * to a {@link DeviceBusController}, which in turn is usually used by VMs to access
 * these devices.
 */
public interface DeviceBus {
    /**
     * Adds a device to this device bus.
     * <p>
     * Adding a device to the bus does <em>not</em> transfer ownership. In particular,
     * the bus will not handle persisting devices that have been added to it. Also,
     * the bus does not persist the list of devices. Instead, all devices that have
     * been added to the bus must be added again after a load. It is the responsibility
     * of {@link DeviceBusElement}s to detect and add devices to the bus.
     *
     * @param device the device to add to the bus.
     */
    void addDevice(Device device);

    /**
     * Removes a device from this device bus.
     * <p>
     * If the device has not been added with {@link #addDevice(Device)} before calling
     * this method, this method is a no-op.
     *
     * @param device the device to remove from the bus.
     */
    void removeDevice(Device device);

    /**
     * The list of all devices currently registered with this device bus.
     *
     * @return the list of all devices that are currently on this bus.
     */
    Collection<Device> getDevices();

    /**
     * Schedules a rescan of the device bus.
     * <p>
     * This will cause the internal device bus controller to discard the current bus
     * state and scan for connected bus segments at an unspecified time in the future
     * (typically during the next tick).
     * <p>
     * This should be called on all neighboring {@link DeviceBus} instances when a
     * {@link DeviceBus} is created, typically when a block is placed/runs its first
     * update after a load.
     * <p>
     * Technically this is a convenience method. It is equivalent to querying for a
     * {@link DeviceBusElement}, checking if a controller is set and then scheduling
     * a scan on the controller, if present. This way regular code will only ever
     * have to interact with this interface.
     */
    void scheduleScan();
}

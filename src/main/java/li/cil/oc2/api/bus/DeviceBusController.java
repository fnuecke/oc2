package li.cil.oc2.api.bus;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.IdentifiableDevice;

import java.util.Collection;

/**
 * For each device bus there can be exactly one controller. The controller performs the
 * actual scan for adjacent {@link DeviceBusElement}s and registers itself with them via
 * {@link DeviceBusElement#setController(DeviceBusController)}.
 * <p>
 * This interface is usually provided by VM containers and used to collect connected
 * {@link Device}s by aggregating the devices that were added to the device bus elements
 * via {@link DeviceBus#addDevice(IdentifiableDevice)}.
 * <p>
 * The only way for {@link DeviceBusElement}s to be added to a bus is for a
 * {@link DeviceBusController} to detect them during a scan.
 * <p>
 * This interface is only of relevance when implementing a VM container or a bus element,
 * i.e. something that acts as a "cable" or otherwise extends the bus itself.
 *
 * @see DeviceBusElement
 */
public interface DeviceBusController {
    /**
     * Schedules a scan.
     * <p>
     * This will immediately invalidate the current bus, i.e. all {@link DeviceBusElement}s
     * will be removed from the controller and {@link #getDevices()} will return an empty
     * list after this call.
     * <p>
     * Multiple sequential calls to this method do nothing, the actual scan will be performed
     * in the next update.
     */
    void scheduleBusScan();

    /**
     * Forces a device map rebuild.
     * <p>
     * This causes the controller to query all registered {@link DeviceBusElement}s for their
     * current devices and update the aggregated list of devices. Unlike {@link #scheduleBusScan()}
     * this operation runs synchronously. The list of devices known to the controller will be
     * updated when this method returns.
     * <p>
     * This should be called when the list of devices of a {@link DeviceBusElement} changes.
     */
    void scanDevices();

    /**
     * The list of all devices currently known to this controller.
     * <p>
     * This is the aggregation of all {@link Device} added to all {@link DeviceBusElement}s known
     * to the controller as found during the last scan scheduled via {@link #scheduleBusScan()}.
     *
     * @return the list of all devices on the bus managed by this controller.
     */
    Collection<IdentifiableDevice> getDevices();
}

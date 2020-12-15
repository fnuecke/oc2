package li.cil.oc2.api.bus.device.vm;

/**
 * Specialization of {@link VMDevice} for devices that require additional
 * lifecycle events to function correctly.
 * <p>
 * A typical use case is to add custom synchronization points for asynchronous
 * operations, such as loading blobs, and for disposing unmanaged resources
 * acquired in {@link VMDevice#load(VMContext)}.
 */
public interface VMDeviceLifecycleListener extends VMDevice {
    /**
     * Called to notify the device of a lifecycle event.
     *
     * @param event the type of the event.
     */
    void handleLifecycleEvent(VMDeviceLifecycleEventType event);
}

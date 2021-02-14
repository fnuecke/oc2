package li.cil.oc2.api.bus.device.vm.context;

/**
 * Allows registering for {@link li.cil.oc2.api.bus.device.vm.event.VMLifecycleEvent}s.
 */
public interface VMLifecycleEventBus {
    /**
     * Registers the specified object as a subscriber for events.
     *
     * @param subscriber the object to subscribe methods of.
     */
    void register(Object subscriber);
}

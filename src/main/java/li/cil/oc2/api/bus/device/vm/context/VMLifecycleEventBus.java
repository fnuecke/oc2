package li.cil.oc2.api.bus.device.vm.context;

/**
 * Allows registering for VM lifecycle events.
 * @see li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent
 * @see li.cil.oc2.api.bus.device.vm.event.VMResumingRunningEvent
 * @see li.cil.oc2.api.bus.device.vm.event.VMResumedRunningEvent
 * @see li.cil.oc2.api.bus.device.vm.event.VMPausingEvent
 */
public interface VMLifecycleEventBus {
    /**
     * Registers the specified object as a subscriber for events.
     *
     * @param subscriber the object to subscribe methods of.
     */
    void register(Object subscriber);
}

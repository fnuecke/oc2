package li.cil.oc2.api.bus.device.vm.event;

/**
 * Fired when the VM resumed running.
 * <p>
 * Fired after {@link VMResumingRunningEvent} has been fired and handled by all devices.
 * <p>
 * Allows device initialization that relies on all other devices having fully loaded.
 * <p>
 * Typically, this is used in combination with {@link VMPausingEvent}, to re-enable external
 * interactions after VM state is guaranteed to be safe to modify again.
 */
public final class VMResumedRunningEvent { }

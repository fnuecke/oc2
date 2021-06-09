package li.cil.oc2.api.bus.device.vm.event;

/**
 * Fired when the VM is paused, typically before state is persisted.
 * <p>
 * Allows devices that offer interaction to external code-flow to suspend
 * such interactions until {@link VMResumedRunningEvent} is fired. This is required
 * if such interactions may modify VM state, to prevent corrupting data being
 * serialized asynchronously.
 */
public final class VMPausingEvent {
}

package li.cil.oc2.api.bus.device.vm.event;

import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;

/**
 * Fired when the VM resumes running.
 * <p>
 * Fired after all devices reported success from {@link VMDevice#load(VMContext)}.
 * <p>
 * Fired on initial boot-up as well as when the VM resumes after being restored
 * from a saved state as well as when continuing to run after being paused for
 * a save. It is intended for awaiting asynchronous load and store operations.
 */
public final class VMResumingRunningEvent extends VMLifecycleEvent {
}

package li.cil.oc2.api.bus.device.vm;

public enum VMLifecycleEventType {
    /**
     * Reported when the VM resumes running.
     * <p>
     * Fired after all devices reported success from {@link VMDevice#load(VMContext)}.
     * <p>
     * Fired on initial boot-up as well as when the VM resumes after being restored
     * from a saved state. It is intended for awaiting asynchronous load operations.
     */
    RESUME_RUNNING,

    /**
     * Reported exactly once, when the VM first starts running.
     * <p>
     * Fired after all devices reported success from {@link VMDevice#load(VMContext)}.
     * <p>
     * If a running VM is restored from a saved state, this event will not be fired. It is
     * intended for initializing the VM state on boot, e.g. by loading initial executable
     * code into memory.
     * <p>
     * <em>This is invoked from the worker thread running the VM.</em>
     */
    INITIALIZE,
}

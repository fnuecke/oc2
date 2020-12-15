package li.cil.oc2.api.bus.device.vm;

public enum VMDeviceLifecycleEventType {
    /**
     * Fired when the VM resumes running.
     * <p>
     * Fired after all devices reported success from {@link VMDevice#load(VMContext)}.
     * <p>
     * Fired on initial boot-up as well as when the VM resumes after being restored
     * from a saved state. It is intended for awaiting asynchronous load operations.
     */
    RESUME_RUNNING,

    /**
     * Fired exactly once, when the VM first starts running.
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

    /**
     * Fired when the device is disposed, either because the VM is disposed or the source
     * of the device is disconnected / removed from the current VM.
     * <p>
     * Intended for releasing resources acquired in {@link VMDevice#load(VMContext)}.
     */
    UNLOAD,
}

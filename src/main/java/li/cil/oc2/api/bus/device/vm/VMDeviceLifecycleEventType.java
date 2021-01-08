package li.cil.oc2.api.bus.device.vm;

public enum VMDeviceLifecycleEventType {
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
    INITIALIZING,

    /**
     * Fired when the VM is paused, typically before state is persisted.
     * <p>
     * Allows devices that offer interaction to external code-flow to suspend
     * such interactions until {@link #RESUMED_RUNNING} is fired. This is required
     * if such interactions may modify VM state, to prevent corrupting data being
     * serialized asynchronously.
     */
    PAUSING,

    /**
     * Fired when the VM resumes running.
     * <p>
     * Fired after all devices reported success from {@link VMDevice#load(VMContext)}.
     * <p>
     * Fired on initial boot-up as well as when the VM resumes after being restored
     * from a saved state as well as when continuing to run after being paused for
     * a save. It is intended for awaiting asynchronous load and store operations.
     */
    RESUME_RUNNING,

    /**
     * Fired when the VM resumed running.
     * <p>
     * Fired after {@link #RESUME_RUNNING} has been fired and handled by all devices.
     * <p>
     * Allows device initialization that relies on all other devices having fully loaded.
     * <p>
     * Typically this is used in combination with {@link #PAUSING}, to re-enable external
     * interactions after VM state is guaranteed to be safe to modify again.
     */
    RESUMED_RUNNING,

    /**
     * Fired when the device is disposed, either because the VM is disposed or the source
     * of the device is disconnected / removed from the current VM.
     * <p>
     * Intended for releasing resources acquired in {@link VMDevice#load(VMContext)}.
     */
    UNLOAD,
}

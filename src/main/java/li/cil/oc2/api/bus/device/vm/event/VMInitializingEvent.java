package li.cil.oc2.api.bus.device.vm.event;

import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.context.VMContext;

/**
 * Fired exactly once, when the VM first starts running.
 * <p>
 * Fired after all devices reported success from {@link VMDevice#load(VMContext)}.
 * <p>
 * If a running VM is restored from a saved state, this event will not be fired. It is
 * intended for initializing the VM state on boot, e.g. by loading initial executable
 * code into memory.
 * <p>
 * Listeners of this event may throw a {@link VMInitializationException} in case
 * initialization fails. For some devices it may be too costly to perform a full
 * validity check in {@link VMDevice#load(VMContext)}. These devices may still cause
 * a startup to fail this way.
 * <p>
 * <em>This is invoked from the worker thread running the VM.</em>
 */
public final class VMInitializingEvent {
    private final long programStartAddress;

    ///////////////////////////////////////////////////////////////

    public VMInitializingEvent(final long programStartAddress) {
        this.programStartAddress = programStartAddress;
    }

    ///////////////////////////////////////////////////////////////

    /**
     * The address where code execution will begin.
     * <p>
     * Some VM implementations may perform some early setup before jumping to this
     * memory address.
     *
     * @return the memory address where code execution begins.
     */
    public long getProgramStartAddress() {
        return programStartAddress;
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.event;

import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.sedna.api.memory.MemoryMap;

/**
 * Fired exactly once, when the VM first starts running.
 * <p>
 * Fired after all devices reported success from {@link VMDevice#mount(VMContext)}.
 * <p>
 * If a running VM is restored from a saved state, this event will <em>not</em> be fired.
 * It is intended for initializing the VM state on boot, e.g. by loading initial executable
 * code into memory.
 * <p>
 * {@link #memory()} is where execution will begin, which is not always the machine's main memory:
 * a machine that resets into ROM will pass it along here. Writing past the end of it fails, and
 * firmware that is too large for a machine should report failure in that case.
 * <p>
 * Listeners of this event may throw a {@link VMInitializationException} in case
 * initialization fails. For some devices it may be too costly to perform a full
 * validity check in {@link VMDevice#mount(VMContext)}. These devices may still cause
 * a startup to fail this way.
 * <p>
 * <em>This is invoked from the worker thread running the VM.</em>
 */
public record VMInitializingEvent(MemoryMap memory, long programStartAddress) {
}

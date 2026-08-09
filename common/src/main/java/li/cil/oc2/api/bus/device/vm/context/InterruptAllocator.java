/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.bus.device.vm.context;

import li.cil.oc2.api.bus.device.vm.VMDevice;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Allows reserving interrupts on the primary interrupt controller of a virtual machine
 * during a {@link VMDevice#mount(VMContext)} call.
 * <p>
 * Allocated interrupts should be persisted and used in {@link #claimInterrupt(int)}
 * when restoring from a saved state to ensure correct behaviour of the loaded virtual
 * machine.
 */
public interface InterruptAllocator {
    /**
     * Tries to reserve an interrupt with the specified index. This may fail if the
     * interrupt has already been claimed. Use {@link #claimInterrupt()} to obtain
     * a free interrupt.
     *
     * @param interrupt the interrupt to claim.
     * @return {@code true} if the interrupt could be claimed; {@code false} otherwise.
     */
    boolean claimInterrupt(int interrupt);

    /**
     * Tries to claim the next free interrupt. If no more interrupts are available,
     * this will return {@link Optional#empty()}.
     *
     * @return the interrupt that was claimed, if any.
     */
    OptionalInt claimInterrupt();
}

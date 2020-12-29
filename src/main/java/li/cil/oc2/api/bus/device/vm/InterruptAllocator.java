package li.cil.oc2.api.bus.device.vm;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Allows reserving interrupts on the primary interrupt controller of a virtual machine
 * during a {@link VMDevice#load(VMContext)} call.
 * <p>
 * Allocated interrupts should be persisted and used in {@link #claimInterrupt(int)}
 * when restoring from a saved state to ensure correct behaviour of the loaded virtual
 * machine.
 */
public interface InterruptAllocator {
    /**
     * Tries to reserve an interrupt with the specified index. The returned interrupt
     * may differ from the one provided, if the interrupt has already been claimed by
     * some other device. In this case, the result will be same as calling {@link #claimInterrupt()}.
     *
     * @param interrupt the interrupt to claim.
     * @return the interrupt that was claimed, if any.
     */
    OptionalInt claimInterrupt(int interrupt);

    /**
     * Tries to claim the next free interrupt. If no more interrupts are available,
     * this will return {@link Optional#empty()}.
     *
     * @return the interrupt that was claimed, if any.
     */
    OptionalInt claimInterrupt();
}

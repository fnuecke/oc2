package li.cil.oc2.common.vm.context.global;

import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.oc2.common.vm.context.InterruptManager;
import li.cil.oc2.common.vm.context.InterruptValidator;

import java.util.BitSet;
import java.util.OptionalInt;

final class GlobalInterruptAllocator implements InterruptAllocator, InterruptValidator, InterruptManager {
    private final BitSet claimedInterrupts = new BitSet();
    private final BitSet reservedInterrupts;
    private final int interruptCount;
    private int managedMask;

    ///////////////////////////////////////////////////////////////////

    public GlobalInterruptAllocator(final int interruptCount, final BitSet reservedInterrupts) {
        this.reservedInterrupts = reservedInterrupts;
        this.interruptCount = interruptCount;

        // Interrupt zero appears to be evil, so block it.
        this.claimedInterrupts.set(0);
    }

    ///////////////////////////////////////////////////////////////////

    public BitSet getClaimedInterrupts() {
        return claimedInterrupts;
    }

    @Override
    public boolean claimInterrupt(final int interrupt) {
        if (interrupt < 1 || interrupt >= interruptCount) {
            throw new IllegalArgumentException();
        }

        if (claimedInterrupts.get(interrupt)) {
            return false;
        }

        claimedInterrupts.set(interrupt);
        managedMask |= (1 << interrupt);
        return true;
    }

    @Override
    public OptionalInt claimInterrupt() {
        final BitSet allClaimedInterrupts = new BitSet();
        allClaimedInterrupts.or(claimedInterrupts);
        allClaimedInterrupts.or(reservedInterrupts);

        final int interrupt = allClaimedInterrupts.nextClearBit(0);
        if (interrupt >= interruptCount) {
            return OptionalInt.empty();
        }

        claimedInterrupts.set(interrupt);
        managedMask |= (1 << interrupt);

        return OptionalInt.of(interrupt);
    }

    @Override
    public boolean isMaskValid(final int mask) {
        return (mask & ~managedMask) == 0;
    }

    @Override
    public int getMaskedInterrupts(final int interrupts) {
        return interrupts & managedMask;
    }

    @Override
    public int getInterruptCount() {
        return interruptCount;
    }

    @Override
    public void releaseInterrupts(final BitSet interrupts) {
        claimedInterrupts.andNot(interrupts);

        int interrupt = interrupts.nextSetBit(0);
        while (interrupt >= 0) {
            managedMask &= ~(1 << interrupt);
            interrupt = interrupts.nextSetBit(interrupt + 1);
        }
    }
}

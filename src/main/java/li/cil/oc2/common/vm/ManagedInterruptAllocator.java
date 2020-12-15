package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;

import java.util.BitSet;
import java.util.OptionalInt;

public final class ManagedInterruptAllocator implements InterruptAllocator {
    private final BitSet claimedInterrupts;
    private final BitSet reservedInterrupts;
    private final BitSet managedInterrupts;
    private final int interruptCount;
    private boolean isFrozen;
    private int managedMask;

    ///////////////////////////////////////////////////////////////////

    public ManagedInterruptAllocator(final BitSet claimedInterrupts, final BitSet reservedInterrupts, final int interruptCount) {
        this.claimedInterrupts = claimedInterrupts;
        this.reservedInterrupts = reservedInterrupts;
        this.managedInterrupts = new BitSet(interruptCount);
        this.interruptCount = interruptCount;
    }

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        claimedInterrupts.andNot(managedInterrupts);
        managedInterrupts.clear();
        managedMask = 0;
    }

    public boolean isMaskValid(final int mask) {
        return (mask & ~managedMask) == 0;
    }

    @Override
    public OptionalInt claimInterrupt(final int interrupt) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        if (interrupt < 1 || interrupt >= R5PlatformLevelInterruptController.INTERRUPT_COUNT) {
            throw new IllegalArgumentException();
        }

        if (claimedInterrupts.get(interrupt)) {
            return claimInterrupt();
        } else {
            claimedInterrupts.set(interrupt);
            reservedInterrupts.set(interrupt);
            managedInterrupts.set(interrupt);
            managedMask |= (1 << interrupt);
            return OptionalInt.of(interrupt);
        }
    }

    @Override
    public OptionalInt claimInterrupt() {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        final BitSet allClaimedInterrupts = new BitSet();
        allClaimedInterrupts.or(claimedInterrupts);
        allClaimedInterrupts.or(reservedInterrupts);

        final int interrupt = allClaimedInterrupts.nextClearBit(0);
        if (interrupt >= interruptCount) {
            return OptionalInt.empty();
        }

        claimedInterrupts.set(interrupt);
        reservedInterrupts.set(interrupt);
        managedInterrupts.set(interrupt);
        managedMask |= (1 << interrupt);

        return OptionalInt.of(interrupt);
    }
}

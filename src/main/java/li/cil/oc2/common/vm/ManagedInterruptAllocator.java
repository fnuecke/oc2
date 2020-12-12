package li.cil.oc2.common.vm;

import li.cil.oc2.api.bus.device.vm.InterruptAllocator;
import li.cil.sedna.riscv.device.R5PlatformLevelInterruptController;

import java.util.BitSet;
import java.util.OptionalInt;

public final class ManagedInterruptAllocator implements InterruptAllocator {
    private final BitSet interrupts;
    private final BitSet reservedInterrupts;
    private final BitSet managedInterrupts;
    private final int interruptCount;
    private boolean isFrozen;
    private int managedMask;

    ///////////////////////////////////////////////////////////////////

    public ManagedInterruptAllocator(final BitSet interrupts, final BitSet reservedInterrupts, final int interruptCount) {
        this.interrupts = interrupts;
        this.reservedInterrupts = reservedInterrupts;
        this.managedInterrupts = new BitSet(interruptCount);
        this.interruptCount = interruptCount;
    }

    public void freeze() {
        final long[] words = managedInterrupts.toLongArray();
        managedMask = words.length > 0 ? (int) words[0] : 0;
        isFrozen = true;
    }

    public void invalidate() {
        interrupts.andNot(managedInterrupts);
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

        if (interrupt < 0 || interrupt >= R5PlatformLevelInterruptController.INTERRUPT_COUNT) {
            throw new IllegalArgumentException();
        }

        if (interrupts.get(interrupt)) {
            return claimInterrupt();
        } else {
            interrupts.set(interrupt);
            reservedInterrupts.set(interrupt);
            managedInterrupts.set(interrupt);
            return OptionalInt.of(interrupt);
        }
    }

    @Override
    public OptionalInt claimInterrupt() {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        final BitSet claimedInterrupts = new BitSet();
        claimedInterrupts.or(interrupts);
        claimedInterrupts.or(reservedInterrupts);

        final int interrupt = claimedInterrupts.nextClearBit(0);
        if (interrupt >= interruptCount) {
            return OptionalInt.empty();
        }

        interrupts.set(interrupt);
        reservedInterrupts.set(interrupt);
        managedInterrupts.set(interrupt);

        return OptionalInt.of(interrupt);
    }
}

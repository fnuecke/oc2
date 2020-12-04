package li.cil.oc2.common.vm;

import li.cil.oc2.api.vm.InterruptAllocator;

import java.util.BitSet;
import java.util.OptionalInt;

public final class InterruptAllocatorImpl implements InterruptAllocator {
    private final BitSet interrupts;
    private final int interruptCount;
    private boolean isValid = true;

    public InterruptAllocatorImpl(final BitSet interrupts, final int interruptCount) {
        this.interrupts = new BitSet(interruptCount);
        this.interrupts.or(interrupts);
        this.interruptCount = interruptCount;
    }

    public BitSet complete() {
        isValid = false;
        return interrupts;
    }

    @Override
    public OptionalInt claimInterrupt(final int interrupt) {
        if (!isValid) {
            return OptionalInt.empty();
        }

        if (interrupts.get(interrupt)) {
            return claimInterrupt();
        } else {
            interrupts.set(interrupt);
            return OptionalInt.of(interrupt);
        }
    }

    @Override
    public OptionalInt claimInterrupt() {
        if (!isValid) {
            return OptionalInt.empty();
        }

        final int interrupt = interrupts.nextClearBit(0);
        if (interrupt >= interruptCount) {
            return OptionalInt.empty();
        }

        interrupts.set(interrupt);
        return OptionalInt.of(interrupt);
    }
}

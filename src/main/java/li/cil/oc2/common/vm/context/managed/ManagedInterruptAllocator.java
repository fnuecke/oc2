/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.managed;

import li.cil.oc2.api.bus.device.vm.context.InterruptAllocator;
import li.cil.oc2.common.vm.context.InterruptManager;
import li.cil.oc2.common.vm.context.InterruptValidator;

import java.util.BitSet;
import java.util.OptionalInt;

final class ManagedInterruptAllocator implements InterruptAllocator, InterruptValidator {
    private final InterruptAllocator parent;
    private final InterruptManager interruptManager;
    private final BitSet managedInterrupts;
    private final int interruptCount;
    private boolean isFrozen;
    private int managedMask;

    ///////////////////////////////////////////////////////////////////

    public ManagedInterruptAllocator(final InterruptAllocator parent, final InterruptManager interruptManager) {
        this.parent = parent;
        this.interruptManager = interruptManager;
        this.interruptCount = interruptManager.getInterruptCount();
        this.managedInterrupts = new BitSet(interruptCount);
    }

    ///////////////////////////////////////////////////////////////////

    public void freeze() {
        isFrozen = true;
    }

    public void invalidate() {
        interruptManager.releaseInterrupts(managedInterrupts);
        managedInterrupts.clear();
        managedMask = 0;
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
    public boolean claimInterrupt(final int interrupt) {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        if (interrupt < 1 || interrupt >= interruptCount) {
            throw new IllegalArgumentException();
        }

        if (!parent.claimInterrupt(interrupt)) {
            return false;
        }

        managedInterrupts.set(interrupt);
        managedMask |= (1 << interrupt);
        return true;
    }

    @Override
    public OptionalInt claimInterrupt() {
        if (isFrozen) {
            throw new IllegalStateException();
        }

        final OptionalInt result = parent.claimInterrupt();
        result.ifPresent(interrupt -> {
            managedInterrupts.set(interrupt);
            managedMask |= (1 << interrupt);
        });
        return result;
    }
}

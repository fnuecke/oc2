/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm.context.global;

import li.cil.oc2.common.vm.context.InterruptValidator;
import li.cil.sedna.api.device.InterruptController;

final class GlobalInterruptController implements InterruptController {
    private final InterruptController parent;
    private final InterruptValidator validator;
    private int raisedInterruptMask;

    ///////////////////////////////////////////////////////////////////

    public GlobalInterruptController(final InterruptController parent, final InterruptValidator validator) {
        this.parent = parent;
        this.validator = validator;
        raisedInterruptMask = validator.getMaskedInterrupts(parent.getRaisedInterrupts());
    }

    ///////////////////////////////////////////////////////////////////

    public void invalidate() {
        parent.lowerInterrupts(raisedInterruptMask);
        raisedInterruptMask = 0;
    }

    @Override
    public Object getIdentity() {
        return parent.getIdentity();
    }

    @Override
    public void raiseInterrupts(final int mask) {
        if (validator.isMaskValid(mask)) {
            parent.raiseInterrupts(mask);
            raisedInterruptMask |= mask;
        } else {
            throw new IllegalArgumentException("Trying to raise interrupt not allocated by this context.");
        }
    }

    @Override
    public void lowerInterrupts(final int mask) {
        if (validator.isMaskValid(mask)) {
            parent.lowerInterrupts(mask);
            raisedInterruptMask &= ~mask;
        } else {
            throw new IllegalArgumentException("Trying to lower interrupt not allocated by this context.");
        }
    }

    @Override
    public int getRaisedInterrupts() {
        return raisedInterruptMask;
    }
}

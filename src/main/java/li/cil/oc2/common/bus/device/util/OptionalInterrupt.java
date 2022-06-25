/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.vm.context.VMContext;

import java.util.OptionalInt;

public final class OptionalInterrupt {
    private Integer value;

    public boolean isPresent() {
        return value != null;
    }

    public int getAsInt() {
        return value;
    }

    public void set(final int value) {
        this.value = value;
    }

    public void clear() {
        this.value = null;
    }

    public boolean claim(final VMContext context) {
        final OptionalInt claimedInterrupt;
        if (value != null && context.getInterruptAllocator().claimInterrupt(value)) {
            claimedInterrupt = OptionalInt.of(value);
        } else {
            claimedInterrupt = context.getInterruptAllocator().claimInterrupt();
        }

        if (claimedInterrupt.isPresent()) {
            value = claimedInterrupt.getAsInt();
            return true;
        } else {
            return false;
        }
    }
}

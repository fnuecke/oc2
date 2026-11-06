/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.transaction.TransactionContext;

public record CachedFluidHandler(CapabilityCache<FluidHandler> cache) implements FluidHandler {
    @Override
    public int getTanks() {
        return handler().getTanks();
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return handler().getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(final int tank) {
        return handler().getTankCapacity(tank);
    }

    @Override
    public int fill(final FluidStack stack, final TransactionContext transaction) {
        return handler().fill(stack, transaction);
    }

    @Override
    public FluidStack drain(final FluidStack stack, final TransactionContext transaction) {
        return handler().drain(stack, transaction);
    }

    @Override
    public FluidStack drain(final int amount, final TransactionContext transaction) {
        return handler().drain(amount, transaction);
    }

    private FluidHandler handler() {
        final FluidHandler handler = cache.get();
        if (handler == null) {
            throw new IllegalStateException("fluid container is gone");
        }
        return handler;
    }
}

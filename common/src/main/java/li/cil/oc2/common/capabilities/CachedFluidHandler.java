/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;

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
    public int fill(final FluidStack stack, final boolean simulate) {
        return handler().fill(stack, simulate);
    }

    @Override
    public FluidStack drain(final FluidStack stack, final boolean simulate) {
        return handler().drain(stack, simulate);
    }

    @Override
    public FluidStack drain(final int amount, final boolean simulate) {
        return handler().drain(amount, simulate);
    }

    private FluidHandler handler() {
        final FluidHandler handler = cache.get();
        if (handler == null) {
            throw new IllegalStateException("fluid container is gone");
        }
        return handler;
    }
}

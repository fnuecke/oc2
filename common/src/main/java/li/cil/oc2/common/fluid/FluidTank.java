/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;

public final class FluidTank implements FluidHandler {
    private final int capacity;
    private FluidStack fluid = FluidStack.EMPTY;

    // --------------------------------------------------------------------- //

    public FluidTank(final int capacity) {
        this.capacity = capacity;
    }

    // --------------------------------------------------------------------- //

    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        if (fluid.isEmpty()) {
            return new CompoundTag();
        }
        return (CompoundTag) FluidStack.CODEC.encodeStart(provider.createSerializationContext(NbtOps.INSTANCE), fluid).getOrThrow();
    }

    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag tag) {
        fluid = tag.isEmpty()
            ? FluidStack.EMPTY
            : FluidStack.CODEC.parse(provider.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(FluidStack.EMPTY);
    }

    // --------------------------------------------------------------------- //

    public FluidStack getFluid() {
        return fluid;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return fluid;
    }

    @Override
    public int getTankCapacity(final int tank) {
        return capacity;
    }

    @Override
    public int fill(final FluidStack stack, final boolean simulate) {
        if (stack.isEmpty() || (!fluid.isEmpty() && !fluid.isSameFluid(stack))) {
            return 0;
        }

        final int filled = Math.min(capacity - fluid.amount(), stack.amount());
        if (!simulate && filled > 0) {
            fluid = stack.withAmount(fluid.amount() + filled);
        }
        return filled;
    }

    @Override
    public FluidStack drain(final FluidStack stack, final boolean simulate) {
        if (!fluid.isSameFluid(stack)) {
            return FluidStack.EMPTY;
        }
        return drain(stack.amount(), simulate);
    }

    @Override
    public FluidStack drain(final int amount, final boolean simulate) {
        final int drained = Math.min(amount, fluid.amount());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }

        final FluidStack result = fluid.withAmount(drained);
        if (!simulate) {
            fluid = drained == fluid.amount() ? FluidStack.EMPTY : fluid.withAmount(fluid.amount() - drained);
        }
        return result;
    }
}

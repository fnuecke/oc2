/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import li.cil.oc2.common.transaction.SnapshotJournal;
import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FluidTank implements FluidHandler {
    private final int capacity;
    private FluidStack fluid = FluidStack.EMPTY;
    private final SnapshotJournal journal = SnapshotJournal.create(() -> fluid, value -> fluid = value, () -> {});

    // --------------------------------------------------------------------- //

    public FluidTank(final int capacity) {
        this.capacity = capacity;
    }

    // --------------------------------------------------------------------- //

    public void serialize(final ValueOutput output, final String key) {
        if (!fluid.isEmpty()) {
            output.store(key, FluidStack.CODEC, fluid);
        }
    }

    public void deserialize(final ValueInput input, final String key) {
        fluid = input.read(key, FluidStack.CODEC).orElse(FluidStack.EMPTY);
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
    public int fill(final FluidStack stack, final TransactionContext transaction) {
        if (stack.isEmpty() || (!fluid.isEmpty() && !fluid.isSameFluid(stack))) {
            return 0;
        }

        final int filled = Math.min(capacity - fluid.amount(), stack.amount());
        if (filled > 0) {
            journal.updateSnapshots(transaction);
            fluid = stack.withAmount(fluid.amount() + filled);
        }
        return filled;
    }

    @Override
    public FluidStack drain(final FluidStack stack, final TransactionContext transaction) {
        if (!fluid.isSameFluid(stack)) {
            return FluidStack.EMPTY;
        }
        return drain(stack.amount(), transaction);
    }

    @Override
    public FluidStack drain(final int amount, final TransactionContext transaction) {
        final int drained = Math.min(amount, fluid.amount());
        if (drained <= 0) {
            return FluidStack.EMPTY;
        }

        final FluidStack result = fluid.withAmount(drained);
        journal.updateSnapshots(transaction);
        fluid = drained == fluid.amount() ? FluidStack.EMPTY : fluid.withAmount(fluid.amount() - drained);
        return result;
    }
}

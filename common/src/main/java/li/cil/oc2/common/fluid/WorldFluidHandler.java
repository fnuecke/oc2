/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import li.cil.oc2.common.transaction.SnapshotJournal;
import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.Optional;

public final class WorldFluidHandler implements FluidHandler {
    private final Level level;
    private final BlockPos pos;
    private Optional<FluidStack> target = Optional.empty();
    private final SnapshotJournal journal = SnapshotJournal.create(() -> target, value -> target = value, this::applyTarget);

    // --------------------------------------------------------------------- //

    private WorldFluidHandler(final Level level, final BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    @Nullable
    public static FluidHandler of(final Level level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (!state.isAir() && !state.canBeReplaced()
            && !(state.getBlock() instanceof LiquidBlockContainer)
            && !(state.getBlock() instanceof BucketPickup)) {
            return null;
        }
        return new WorldFluidHandler(level, pos);
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return target.orElseGet(this::getWorldFluid);
    }

    @Override
    public int getTankCapacity(final int tank) {
        return BUCKET;
    }

    @Override
    public int fill(final FluidStack stack, final TransactionContext transaction) {
        if (!getFluidInTank(0).isEmpty() || stack.amount() < BUCKET || !stack.components().isEmpty()
            || !(stack.fluid() instanceof final FlowingFluid fluid)
            || !(fluid.getBucket() instanceof BucketItem)) {
            return 0;
        }

        final FluidStack worldFluid = getWorldFluid();
        final BlockState state = level.getBlockState(pos);
        final boolean canPlace;
        if (worldFluid.isEmpty()) {
            canPlace = state.isAir() || state.canBeReplaced(fluid) || (state.getBlock() instanceof final LiquidBlockContainer container && container.canPlaceLiquid(null, level, pos, state, fluid));
        } else {
            // Defensive check since we cannot assume a non-source block can take any other liquid.
            canPlace = worldFluid.fluid() == fluid || state.getBlock() instanceof LiquidBlock;
        }
        if (!canPlace) {
            return 0;
        }

        setTarget(stack.withAmount(BUCKET), transaction);
        return BUCKET;
    }

    @Override
    public FluidStack drain(final FluidStack stack, final TransactionContext transaction) {
        final FluidStack current = getFluidInTank(0);
        if (!current.isSameFluid(stack)) {
            return FluidStack.EMPTY;
        }
        return drain(stack.amount(), transaction);
    }

    @Override
    public FluidStack drain(final int amount, final TransactionContext transaction) {
        final FluidStack current = getFluidInTank(0);
        if (amount < BUCKET || current.isEmpty()) {
            return FluidStack.EMPTY;
        }

        if (target.isEmpty() && (!(current.fluid().getBucket() instanceof BucketItem)
            || !(level.getBlockState(pos).getBlock() instanceof BucketPickup))) {
            return FluidStack.EMPTY;
        }

        setTarget(FluidStack.EMPTY, transaction);
        return current;
    }

    // --------------------------------------------------------------------- //

    private FluidStack getWorldFluid() {
        final FluidState fluidState = level.getFluidState(pos);
        return fluidState.isSource() ? new FluidStack(fluidState.getType(), BUCKET) : FluidStack.EMPTY;
    }

    private void setTarget(final FluidStack value, final TransactionContext transaction) {
        journal.updateSnapshots(transaction);
        target = value.fluid() == getWorldFluid().fluid() ? Optional.empty() : Optional.of(value);
    }

    private void applyTarget() {
        if (target.isEmpty()) {
            return;
        }

        final BlockState state = level.getBlockState(pos);
        if (!getWorldFluid().isEmpty() && state.getBlock() instanceof final BucketPickup pickup
            && !pickup.pickupBlock(null, level, pos, state).isEmpty()) {
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }

        if (target.get().fluid().getBucket() instanceof final BucketItem bucket) {
            bucket.emptyContents(null, level, pos, null);
        }

        target = Optional.empty();
    }
}

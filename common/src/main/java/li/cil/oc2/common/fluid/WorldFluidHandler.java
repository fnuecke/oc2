/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;

public final class WorldFluidHandler implements FluidHandler {
    private final Level level;
    private final BlockPos pos;

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
        final FluidState fluidState = level.getFluidState(pos);
        return fluidState.isSource() ? new FluidStack(fluidState.getType(), BUCKET) : FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(final int tank) {
        return BUCKET;
    }

    @Override
    public int fill(final FluidStack stack, final boolean simulate) {
        if (stack.amount() < BUCKET || !stack.components().isEmpty()
            || !(stack.fluid() instanceof final FlowingFluid fluid)
            || !(fluid.getBucket() instanceof final BucketItem bucket)
            || level.getFluidState(pos).isSource()) {
            return 0;
        }

        final BlockState state = level.getBlockState(pos);
        final boolean canPlace = state.isAir() || state.canBeReplaced(fluid)
            || (state.getBlock() instanceof final LiquidBlockContainer container && container.canPlaceLiquid(null, level, pos, state, fluid));
        if (!canPlace) {
            return 0;
        }

        if (!simulate && !bucket.emptyContents(null, level, pos, null)) {
            return 0;
        }

        return BUCKET;
    }

    @Override
    public FluidStack drain(final FluidStack stack, final boolean simulate) {
        final FluidStack current = getFluidInTank(0);
        if (!current.isSameFluid(stack)) {
            return FluidStack.EMPTY;
        }
        return drain(stack.amount(), simulate);
    }

    @Override
    public FluidStack drain(final int amount, final boolean simulate) {
        final BlockState state = level.getBlockState(pos);
        final FluidStack current = getFluidInTank(0);
        if (amount < BUCKET || current.isEmpty()
            || !(current.fluid().getBucket() instanceof BucketItem)
            || !(state.getBlock() instanceof final BucketPickup pickup)) {
            return FluidStack.EMPTY;
        }

        if (!simulate) {
            if (pickup.pickupBlock(null, level, pos, state).isEmpty()) {
                return FluidStack.EMPTY;
            }
            level.gameEvent(null, GameEvent.FLUID_PICKUP, pos);
        }

        return current;
    }
}

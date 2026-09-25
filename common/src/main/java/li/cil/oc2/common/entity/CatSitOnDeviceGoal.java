/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.entity;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Cat;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class CatSitOnDeviceGoal extends MoveToBlockGoal {
    private static final int PRIORITY = 7;
    private static final double SPEED_MODIFIER = 0.8;
    private static final int SEARCH_RANGE = 8;

    // --------------------------------------------------------------------- //

    private final Cat cat;

    // --------------------------------------------------------------------- //

    public CatSitOnDeviceGoal(final Cat cat) {
        super(cat, SPEED_MODIFIER, SEARCH_RANGE);
        this.cat = cat;
    }

    // --------------------------------------------------------------------- //

    public static void onEntityLoad(final Entity entity) {
        if (entity instanceof final Cat cat && cat.goalSelector.getAvailableGoals().stream()
            .noneMatch(goal -> goal.getGoal() instanceof CatSitOnDeviceGoal)) {
            cat.goalSelector.addGoal(PRIORITY, new CatSitOnDeviceGoal(cat));
        }
    }

    public static boolean isCatSittingAt(final Level level, final BlockPos pos) {
        for (final Cat cat : level.getEntitiesOfClass(Cat.class, new AABB(pos))) {
            if (cat.isInSittingPose()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        return cat.isTame() && !cat.isOrderedToSit() && super.canUse();
    }

    @Override
    public void start() {
        super.start();
        cat.setInSittingPose(false);
    }

    @Override
    public void stop() {
        super.stop();
        cat.setInSittingPose(false);
    }

    @Override
    public void tick() {
        super.tick();
        if (!isReachedTarget() && super.shouldRecalculatePath()) {
            moveMobToBlock();
        }

        cat.setInSittingPose(isReachedTarget());
        if (isReachedTarget() && mob.level().getBlockState(blockPos).is(Blocks.COMPUTER.get())) {
            cat.getLookControl().setLookAt(Vec3.atCenterOf(blockPos));
        }
    }

    @Override
    public boolean shouldRecalculatePath() {
        return false; // Re-pathed in tick() with accuracy 0.
    }

    // --------------------------------------------------------------------- //

    @Override
    protected boolean isValidTarget(final LevelReader level, final BlockPos pos) {
        final BlockState state = level.getBlockState(pos);
        if (state.is(Blocks.KEYBOARD.get())) {
            return level.isEmptyBlock(pos.above());
        }

        if (state.is(Blocks.COMPUTER.get())) {
            final BlockPos front = pos.relative(state.getValue(ComputerBlock.FACING));
            return level.isEmptyBlock(front) &&
                level.getBlockState(front.below()).isFaceSturdy(level, front.below(), Direction.UP) &&
                level.getBlockEntity(pos) instanceof final ComputerBlockEntity computer &&
                computer.getVirtualMachine().isRunning();
        }

        return false;
    }

    @Override
    protected BlockPos getMoveToTarget() {
        final BlockState state = mob.level().getBlockState(blockPos);
        if (state.is(Blocks.COMPUTER.get())) {
            return blockPos.relative(state.getValue(ComputerBlock.FACING));
        }
        return super.getMoveToTarget();
    }

    @Override
    protected void moveMobToBlock() {
        final PathNavigation navigation = mob.getNavigation();
        navigation.moveTo(navigation.createPath(getMoveToTarget(), 0), speedModifier);
    }
}

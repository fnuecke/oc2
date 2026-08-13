/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotCrushTests {
    private static final BlockPos ROBOT_POS = new BlockPos(16, WORK_Y, 2);

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void crushesAnOrdinaryBlock(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final BlockPos target = robot.blockPosition();
        helper.getLevel().setBlockAndUpdate(target, Blocks.DIRT.defaultBlockState());

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    if (!helper.getLevel().getBlockState(target).isAir()) {
                        throw new GameTestAssertException("the robot did not clear the dirt it was inside of");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void leavesUnbreakableBlocksAlone(final GameTestHelper helper) {
        assertSurvivesTheRobot(helper, Blocks.BEDROCK);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void leavesBarriersAlone(final GameTestHelper helper) {
        assertSurvivesTheRobot(helper, Blocks.BARRIER);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void keepsTheWaterOfAWaterloggedBlock(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final BlockPos target = robot.blockPosition();
        helper.getLevel().setBlockAndUpdate(target, Blocks.OAK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true));

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    final BlockState after = helper.getLevel().getBlockState(target);
                    if (after.is(Blocks.OAK_SLAB)) {
                        throw new GameTestAssertException("the robot did not clear the slab it was inside of");
                    }
                    if (!after.getFluidState().is(Fluids.WATER)) {
                        throw new GameTestAssertException(
                                "the water the slab was logged with is gone; got " + after);
                    }
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------- //

    private static void assertSurvivesTheRobot(final GameTestHelper helper, final Block block) {
        final Robot robot = placeRobot(helper);
        final BlockPos target = robot.blockPosition();
        helper.getLevel().setBlockAndUpdate(target, block.defaultBlockState());

        helper.startSequence()
                .thenExecuteAfter(20, () -> {
                    if (helper.getLevel().getBlockState(target).isAir()) {
                        throw new GameTestAssertException(
                                "the robot ate " + block.getName().getString() + "; it should be stuck instead");
                    }
                })
                .thenSucceed();
    }

    private static Robot placeRobot(final GameTestHelper helper) {
        useOn(helper, fakePlayer(helper), new ItemStack(Items.ROBOT.get()), ROBOT_POS, Direction.UP);

        final AABB bounds = new AABB(helper.absolutePos(ROBOT_POS)).inflate(2);
        final List<Robot> robots = helper.getLevel().getEntitiesOfClass(Robot.class, bounds);
        if (robots.size() != 1) {
            throw new GameTestAssertException("expected exactly one robot near " + ROBOT_POS + ", found " + robots.size());
        }

        return robots.get(0);
    }

    private RobotCrushTests() {
    }
}

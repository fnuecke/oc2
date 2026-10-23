/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.RobotCrushTests;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.RobotCrushTests.ROBOT_CHECK_GRACE_PERIOD;
import static li.cil.oc2.gametest.RobotCrushTests.putBlockInsideTheRobot;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "robot_crush")
public final class RobotCrushTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 120)
    @TestHolder
    public static void crushesAnOrdinaryBlock(final GameTestHelper helper) {
        RobotCrushTests.crushesAnOrdinaryBlock(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 120)
    @TestHolder
    public static void leavesUnbreakableBlocksAlone(final GameTestHelper helper) {
        assertSurvivesTheRobot(helper, Blocks.BEDROCK);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 120)
    @TestHolder
    public static void crushKeepsWaterlogging(final GameTestHelper helper) {
        final BlockPos target = putBlockInsideTheRobot(helper, Blocks.OAK_SLAB.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true));

        helper.startSequence()
            .thenExecuteAfter(ROBOT_CHECK_GRACE_PERIOD, () -> {
                final BlockState after = helper.getLevel().getBlockState(target);
                if (after.is(Blocks.OAK_SLAB)) {
                    throw new GameTestAssertException(Component.literal("the robot did not clear the slab it was inside of"), 0);
                }
                if (!after.getFluidState().is(Fluids.WATER)) {
                    throw new GameTestAssertException(Component.literal("waterlogging is gone; got " + after), 0);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void assertSurvivesTheRobot(final GameTestHelper helper, final Block block) {
        final BlockPos target = putBlockInsideTheRobot(helper, block);

        helper.startSequence()
            .thenExecuteAfter(ROBOT_CHECK_GRACE_PERIOD, () -> {
                if (helper.getLevel().getBlockState(target).isAir()) {
                    throw new GameTestAssertException(Component.literal(
                        "the robot ate " + block.getName().getString() + "; it should be stuck instead"), 0);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private RobotCrushTestsNeoForge() {
    }
}

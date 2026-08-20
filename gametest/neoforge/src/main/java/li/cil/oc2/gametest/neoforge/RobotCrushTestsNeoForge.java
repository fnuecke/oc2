/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.RobotCrushTests;
import li.cil.oc2.gametest.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.RobotCrushTests.ROBOT_CHECK_GRACE_PERIOD;
import static li.cil.oc2.gametest.RobotCrushTests.ROBOT_POS;
import static li.cil.oc2.gametest.RobotCrushTests.putBlockInsideTheRobot;
import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotCrushTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public static void crushesAnOrdinaryBlock(final GameTestHelper helper) {
        RobotCrushTests.crushesAnOrdinaryBlock(helper);
    }

    // ------------------------------------------------------------- //

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
        final BlockPos target = RobotFixture.place(helper, ROBOT_POS).blockPos();
        helper.getLevel().setBlockAndUpdate(target, Blocks.OAK_SLAB.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, true));

        helper.startSequence()
                .thenExecuteAfter(ROBOT_CHECK_GRACE_PERIOD, () -> {
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
        final BlockPos target = putBlockInsideTheRobot(helper, block);

        helper.startSequence()
                .thenExecuteAfter(ROBOT_CHECK_GRACE_PERIOD, () -> {
                    if (helper.getLevel().getBlockState(target).isAir()) {
                        throw new GameTestAssertException(
                                "the robot ate " + block.getName().getString() + "; it should be stuck instead");
                    }
                })
                .thenSucceed();
    }

    // ------------------------------------------------------------- //

    private RobotCrushTestsNeoForge() {
    }
}

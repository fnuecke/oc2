/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.BlockOperationsModuleTests.ROBOT_POS;
import static li.cil.oc2.gametest.util.DeviceCalls.invokeRpc;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotDetectTests {
    @GameTest(template = TEMPLATE)
    public static void solidBlocksReadAsSolid(final GameTestHelper helper) {
        requireDetects(helper, Blocks.STONE.defaultBlockState(), "solid");
    }

    @GameTest(template = TEMPLATE)
    public static void emptySpaceReadsAsAir(final GameTestHelper helper) {
        requireDetects(helper, Blocks.AIR.defaultBlockState(), "air");
    }

    @GameTest(template = TEMPLATE)
    public static void fluidsReadAsFluid(final GameTestHelper helper) {
        requireDetects(helper, Blocks.WATER.defaultBlockState(), "fluid");
    }

    @GameTest(template = TEMPLATE)
    public static void blocksTheRobotCanMoveThroughReadAsAir(final GameTestHelper helper) {
        requireDetects(helper, Blocks.SHORT_GRASS.defaultBlockState(), "air");
    }

    @GameTest(template = TEMPLATE)
    public static void obstructionsWinOverFluid(final GameTestHelper helper) {
        requireDetects(helper, Blocks.CHAIN.defaultBlockState()
            .setValue(BlockStateProperties.WATERLOGGED, true), "solid");
    }

    // --------------------------------------------------------------------- //

    private static void requireDetects(final GameTestHelper helper, final BlockState state, final String expected) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(60, () -> {
                final BlockPos target = robot.frontPos();
                helper.getLevel().setBlockAndUpdate(target, state);

                final String actual = String.valueOf(invokeRpc(robot.devices(), "detect", "front"));
                if (!expected.equals(actual)) {
                    throw new GameTestAssertException("detect() reported \"" + actual + "\" for "
                        + helper.getLevel().getBlockState(target) + ", expected \"" + expected + "\"");
                }
            })
            .thenSucceed();
    }

    private RobotDetectTests() {
    }
}

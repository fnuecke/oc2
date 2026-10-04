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

import static li.cil.oc2.gametest.util.DeviceCalls.invokeRpc;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.ROBOT_POS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RobotDetectTests {
    @GameTest(template = TEMPLATE)
    public static void detectClassifiesTheBlockInFront(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        helper.startSequence()
            .thenExecuteAfter(60, () -> {
                requireDetects(helper, robot, Blocks.STONE.defaultBlockState(), "solid");
                requireDetects(helper, robot, Blocks.AIR.defaultBlockState(), "air");
                requireDetects(helper, robot, Blocks.WATER.defaultBlockState(), "fluid");
                requireDetects(helper, robot, Blocks.SHORT_GRASS.defaultBlockState(), "air");
                requireDetects(helper, robot, Blocks.CHAIN.defaultBlockState()
                    .setValue(BlockStateProperties.WATERLOGGED, true), "solid");
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void requireDetects(final GameTestHelper helper, final RobotFixture robot,
                                       final BlockState state, final String expected) {
        final BlockPos target = robot.frontPos();
        helper.getLevel().setBlockAndUpdate(target, state);

        final String actual = String.valueOf(invokeRpc(robot.devices(), "detect", "front"));
        if (!expected.equals(actual)) {
            throw new GameTestAssertException("detect() reported \"" + actual + "\" for "
                + helper.getLevel().getBlockState(target) + ", expected \"" + expected + "\"");
        }
    }

    // --------------------------------------------------------------------- //

    private RobotDetectTests() {
    }
}

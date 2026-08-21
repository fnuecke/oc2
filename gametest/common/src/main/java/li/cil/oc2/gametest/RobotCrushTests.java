/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static li.cil.oc2.gametest.TestSupport.WORK_Y;

public final class RobotCrushTests {
    public static final BlockPos ROBOT_POS = new BlockPos(16, WORK_Y, 2);
    public static final int ROBOT_CHECK_GRACE_PERIOD = 40; // Give robot check time to kick in.

    // --------------------------------------------------------------------- //

    public static void crushesAnOrdinaryBlock(final GameTestHelper helper) {
        final BlockPos target = putBlockInsideTheRobot(helper, Blocks.DIRT);

        helper.startSequence()
            .thenExecuteAfter(ROBOT_CHECK_GRACE_PERIOD, () -> {
                if (!helper.getLevel().getBlockState(target).isAir()) {
                    throw new GameTestAssertException("the robot did not clear the dirt it was inside of");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    public static BlockPos putBlockInsideTheRobot(final GameTestHelper helper, final Block block) {
        final BlockPos target = RobotFixture.place(helper, ROBOT_POS).blockPos();
        helper.getLevel().setBlockAndUpdate(target, block.defaultBlockState());
        return target;
    }

    // --------------------------------------------------------------------- //

    private RobotCrushTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.BlockOperationsModuleDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

import static li.cil.oc2.gametest.util.TestSupport.WORK_Y;

public final class BlockOperationsModuleTests {
    public static final BlockPos ROBOT_POS = new BlockPos(12, WORK_Y, 2);

    // --------------------------------------------------------------------- //

    public static void excavatesWithACorrectTool(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        final BlockPos target = robot.putBlockInFront(Blocks.STONE);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavating stone with a diamond pickaxe failed");
        }

        if (!helper.getLevel().getBlockState(target).isAir()) {
            throw new GameTestAssertException("excavate reported success but the block is still there");
        }

        if (!robot.has(net.minecraft.world.item.Items.COBBLESTONE)) {
            throw new GameTestAssertException("the block was broken but nothing was collected");
        }

        helper.succeed();
    }

    public static void placesFromTheSelectedSlot(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.STONE, 2));

        final BlockPos target = robot.putBlockInFront(Blocks.AIR);

        if (!module.place(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("placing a stone block failed");
        }

        if (!helper.getLevel().getBlockState(target).is(Blocks.STONE)) {
            throw new GameTestAssertException("place reported success but there is no stone at " + target);
        }

        final ItemStack remaining = robot.selected();
        if (remaining.getCount() != 1) {
            throw new GameTestAssertException("expected exactly one stone to be consumed, slot holds " + remaining);
        }

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    public static BlockOperationsModuleDevice moduleFor(final RobotFixture robot) {
        return new BlockOperationsModuleDevice(
            new ItemStack(Items.BLOCK_OPERATIONS_MODULE.get()), robot.entity(), robot.entity());
    }

    // --------------------------------------------------------------------- //

    private BlockOperationsModuleTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.item.BlockOperationsModuleDevice;
import li.cil.oc2.gametest.BlockOperationsModuleTests;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.BlockOperationsModuleTests.moduleFor;
import static li.cil.oc2.gametest.util.TestSupport.ROBOT_POS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "block_operations_module")
public final class BlockOperationsModuleTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void excavatesWithACorrectTool(final GameTestHelper helper) {
        BlockOperationsModuleTests.excavatesWithACorrectTool(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void placesFromTheSelectedSlot(final GameTestHelper helper) {
        BlockOperationsModuleTests.placesFromTheSelectedSlot(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void refusesWhenTheToolCannotHarvest(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        final BlockPos target = robot.putBlockInFront(Blocks.STONE);

        if (module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException(Component.literal("excavated stone bare-handed; that drops nothing"), 0);
        }

        if (helper.getLevel().getBlockState(target).isAir()) {
            throw new GameTestAssertException(Component.literal("excavate reported failure but broke the block anyway"), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void refusesUnbreakableBlocks(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE));
        final BlockPos target = robot.putBlockInFront(Blocks.BEDROCK);

        if (module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException(Component.literal("excavated bedrock"), 0);
        }

        if (helper.getLevel().getBlockState(target).isAir()) {
            throw new GameTestAssertException(Component.literal("bedrock is gone"), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void excavateDamagesTheTool(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        robot.putBlockInFront(Blocks.STONE);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException(Component.literal("excavating stone with an iron pickaxe failed"), 0);
        }

        final ItemStack tool = robot.selected();
        if (tool.isEmpty() || !tool.is(net.minecraft.world.item.Items.IRON_PICKAXE)) {
            throw new GameTestAssertException(Component.literal("the tool did not come back to its slot: " + tool), 0);
        }

        if (tool.getDamageValue() != 1) {
            throw new GameTestAssertException(Component.literal("expected the pickaxe to take exactly one point of " +
                "damage, got " + tool.getDamageValue()), 0);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void hardBlocksExtendCooldown(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        robot.putBlockInFront(Blocks.OBSIDIAN);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException(Component.literal("excavating obsidian with a diamond pickaxe failed"), 0);
        }

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                robot.putBlockInFront(Blocks.OBSIDIAN);
                if (module.excavate(RobotOperationSide.FRONT)) {
                    throw new GameTestAssertException(Component.literal("should still be on cooldown"), 0);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private BlockOperationsModuleTestsNeoForge() {
    }
}

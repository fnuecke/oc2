/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.BlockOperationsModuleDevice;
import li.cil.oc2.gametest.BlockOperationsModuleTests;
import li.cil.oc2.gametest.fixture.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.BlockOperationsModuleTests.ROBOT_POS;
import static li.cil.oc2.gametest.BlockOperationsModuleTests.moduleFor;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockOperationsModuleTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void excavatesWithACorrectTool(final GameTestHelper helper) {
        BlockOperationsModuleTests.excavatesWithACorrectTool(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void placesFromTheSelectedSlot(final GameTestHelper helper) {
        BlockOperationsModuleTests.placesFromTheSelectedSlot(helper);
    }

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void refusesWhenTheToolCannotHarvest(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        final BlockPos target = robot.putBlockInFront(Blocks.STONE);

        if (module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavated stone bare-handed; that drops nothing");
        }

        if (helper.getLevel().getBlockState(target).isAir()) {
            throw new GameTestAssertException("excavate reported failure but broke the block anyway");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void refusesUnbreakableBlocks(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE));
        final BlockPos target = robot.putBlockInFront(Blocks.BEDROCK);

        if (module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavated bedrock");
        }

        if (helper.getLevel().getBlockState(target).isAir()) {
            throw new GameTestAssertException("bedrock is gone");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wearsOutTheToolRatherThanTheModule(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        robot.putBlockInFront(Blocks.STONE);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavating stone with an iron pickaxe failed");
        }

        final ItemStack tool = robot.selected();
        if (tool.isEmpty() || !tool.is(net.minecraft.world.item.Items.IRON_PICKAXE)) {
            throw new GameTestAssertException("the tool did not come back to its slot: " + tool);
        }

        if (tool.getDamageValue() != 1) {
            throw new GameTestAssertException("expected the pickaxe to take exactly one point of " +
                "damage, got " + tool.getDamageValue());
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void hardBlocksCostMoreThanTheBaseCooldown(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        robot.give(new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        robot.putBlockInFront(Blocks.OBSIDIAN);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavating obsidian with a diamond pickaxe failed");
        }

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                robot.putBlockInFront(Blocks.OBSIDIAN);
                if (module.excavate(RobotOperationSide.FRONT)) {
                    throw new GameTestAssertException("should still be on cooldown");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private BlockOperationsModuleTestsNeoForge() {
    }
}

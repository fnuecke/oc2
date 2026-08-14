/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.BlockOperationsModuleDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockOperationsModuleTests {
    private static final BlockPos ROBOT_POS = new BlockPos(12, WORK_Y, 2);

    // ------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
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

    @GameTest(template = TEMPLATE)
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

    // ------------------------------------------------------------- //

    private static BlockOperationsModuleDevice moduleFor(final RobotFixture robot) {
        return new BlockOperationsModuleDevice(
                new ItemStack(Items.BLOCK_OPERATIONS_MODULE.get()), robot.entity(), robot.entity());
    }

    // ------------------------------------------------------------- //

    private BlockOperationsModuleTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.rpc.item.BlockOperationsModuleDevice;
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
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.List;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class BlockOperationsModuleTests {
    private static final BlockPos ROBOT_POS = new BlockPos(12, WORK_Y, 2);

    @GameTest(template = TEMPLATE)
    public static void excavatesWithACorrectTool(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        giveItem(robot, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        final BlockPos target = putBlockInFront(helper, robot, Blocks.STONE);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavating stone with a diamond pickaxe failed");
        }

        if (!helper.getLevel().getBlockState(target).isAir()) {
            throw new GameTestAssertException("excavate reported success but the block is still there");
        }

        if (!hasItem(robot, net.minecraft.world.item.Items.COBBLESTONE)) {
            throw new GameTestAssertException("the block was broken but nothing was collected");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void refusesWhenTheToolCannotHarvest(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        final BlockPos target = putBlockInFront(helper, robot, Blocks.STONE);

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
        final Robot robot = placeRobot(helper);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        giveItem(robot, new ItemStack(net.minecraft.world.item.Items.NETHERITE_PICKAXE));
        final BlockPos target = putBlockInFront(helper, robot, Blocks.BEDROCK);

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
        final Robot robot = placeRobot(helper);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        giveItem(robot, new ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        putBlockInFront(helper, robot, Blocks.STONE);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavating stone with an iron pickaxe failed");
        }

        final ItemStack tool = robot.getInventory().getStackInSlot(robot.getSelectedSlot());
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
        final Robot robot = placeRobot(helper);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        giveItem(robot, new ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE));
        putBlockInFront(helper, robot, Blocks.OBSIDIAN);

        if (!module.excavate(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("excavating obsidian with a diamond pickaxe failed");
        }

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    putBlockInFront(helper, robot, Blocks.OBSIDIAN);
                    if (module.excavate(RobotOperationSide.FRONT)) {
                        throw new GameTestAssertException("should still be on cooldown");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE)
    public static void placesFromTheSelectedSlot(final GameTestHelper helper) {
        final Robot robot = placeRobot(helper);
        final BlockOperationsModuleDevice module = moduleFor(robot);
        giveItem(robot, new ItemStack(net.minecraft.world.item.Items.STONE, 2));

        final Direction direction = RobotOperationSide.toGlobal(robot, RobotOperationSide.FRONT);
        final BlockPos target = robot.blockPosition().relative(direction);
        helper.getLevel().setBlockAndUpdate(target, Blocks.AIR.defaultBlockState());

        if (!module.place(RobotOperationSide.FRONT)) {
            throw new GameTestAssertException("placing a stone block failed");
        }

        if (!helper.getLevel().getBlockState(target).is(Blocks.STONE)) {
            throw new GameTestAssertException("place reported success but there is no stone at " + target);
        }

        final ItemStack remaining = robot.getInventory().getStackInSlot(robot.getSelectedSlot());
        if (remaining.getCount() != 1) {
            throw new GameTestAssertException("expected exactly one stone to be consumed, slot holds " + remaining);
        }

        helper.succeed();
    }

    // ------------------------------------------------------------- //

    private static BlockOperationsModuleDevice moduleFor(final Robot robot) {
        return new BlockOperationsModuleDevice(
                new ItemStack(Items.BLOCK_OPERATIONS_MODULE.get()), robot, robot);
    }

    private static void giveItem(final Robot robot, final ItemStack tool) {
        robot.getInventory().insertItem(robot.getSelectedSlot(), tool, false);
    }

    private static boolean hasItem(final Robot robot, final net.minecraft.world.item.Item item) {
        final ItemHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).is(item)) {
                return true;
            }
        }

        return false;
    }

    private static BlockPos putBlockInFront(final GameTestHelper helper, final Robot robot, final Block block) {
        final Direction direction = RobotOperationSide.toGlobal(robot, RobotOperationSide.FRONT);
        final BlockPos target = robot.blockPosition().relative(direction);

        final BlockState state = block.defaultBlockState();
        helper.getLevel().setBlockAndUpdate(target, state);

        return target;
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

    private BlockOperationsModuleTests() {
    }
}

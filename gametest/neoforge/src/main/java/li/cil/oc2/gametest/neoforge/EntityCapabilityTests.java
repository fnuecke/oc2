/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.capabilities.TerminalUserProvider;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.EnergyStorage;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.gametest.RobotFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.vehicle.MinecartChest;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityCapabilityTests {
    private static final BlockPos ROBOT_POS = new BlockPos(6, WORK_Y, 2);
    private static final BlockPos MINECART_POS = new BlockPos(8, WORK_Y, 2);

    // ------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void robotProvidesTerminalUsers(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();

        final TerminalUserProvider provider = Capabilities.get(robot, Capabilities.TERMINAL_USER_PROVIDER, null);
        if (provider == null) {
            throw new GameTestAssertException("robot does not expose a terminal user provider; " +
                    "the import/export card cannot work inside one");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void robotProvidesInventoryAndEnergy(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();

        final ItemHandler inventory = Capabilities.get(robot, Capabilities.ITEM_HANDLER, Direction.DOWN);
        if (inventory == null || inventory.getSlots() == 0) {
            throw new GameTestAssertException("robot inventory is not reachable through the capability: " + inventory);
        }

        if (Config.robotsUseEnergy()) {
            final EnergyStorage energy = Capabilities.get(robot, Capabilities.ENERGY_STORAGE, Direction.DOWN);
            if (energy == null) {
                throw new GameTestAssertException("robot energy is not reachable through the capability, " +
                        "so the charger cannot charge it");
            }
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void robotIsVisibleToNeoForgeCapabilities(final GameTestHelper helper) {
        final Robot robot = RobotFixture.place(helper, ROBOT_POS).entity();

        if (robot.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.ENTITY_AUTOMATION, Direction.DOWN) == null) {
            throw new GameTestAssertException("robot inventory is invisible to NeoForge's item handler capability");
        }

        if (Config.robotsUseEnergy() &&
                robot.getCapability(net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ENTITY, Direction.DOWN) == null) {
            throw new GameTestAssertException("robot energy is invisible to NeoForge's energy capability");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void foreignEntityInventoryIsReachable(final GameTestHelper helper) {
        final MinecartChest minecart = helper.spawn(EntityType.CHEST_MINECART, MINECART_POS);
        minecart.setItem(0, new ItemStack(Blocks.STONE, 3));

        final ItemHandler inventory = Capabilities.get(minecart, Capabilities.ITEM_HANDLER, Direction.DOWN);
        if (inventory == null) {
            throw new GameTestAssertException("inventory of a non-OC2 entity is not reachable; " +
                    "the lookup is not bridging to NeoForge's capabilities");
        }

        if (inventory.getStackInSlot(0).getCount() != 3) {
            throw new GameTestAssertException("reached an item handler, but it does not see the minecart's contents");
        }

        helper.succeed();
    }

    // ------------------------------------------------------------- //

    private EntityCapabilityTests() {
    }
}

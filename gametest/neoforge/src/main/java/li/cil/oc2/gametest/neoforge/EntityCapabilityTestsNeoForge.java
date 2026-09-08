/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.gametest.EntityCapabilityTests;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Predicate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class EntityCapabilityTestsNeoForge {
    private static final Predicate<Robot> HAS_ITEM_HANDLER = robot ->
        robot.getCapability(Capabilities.ItemHandler.ENTITY_AUTOMATION, Direction.DOWN) != null;
    private static final Predicate<Robot> HAS_ENERGY_STORAGE = robot ->
        robot.getCapability(Capabilities.EnergyStorage.ENTITY, Direction.DOWN) != null;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void robotProvidesTerminalUsers(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesTerminalUsers(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void robotProvidesInventoryAndEnergy(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesInventoryAndEnergy(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void robotIsVisibleToNeoForgeCapabilities(final GameTestHelper helper) {
        EntityCapabilityTests.robotIsVisibleToPlatformCapabilities(helper, HAS_ITEM_HANDLER, HAS_ENERGY_STORAGE);
    }

    @GameTest(template = TEMPLATE)
    public static void foreignEntityInventoryIsReachable(final GameTestHelper helper) {
        EntityCapabilityTests.foreignEntityInventoryIsReachable(helper);
    }

    // --------------------------------------------------------------------- //

    private EntityCapabilityTestsNeoForge() {
    }
}

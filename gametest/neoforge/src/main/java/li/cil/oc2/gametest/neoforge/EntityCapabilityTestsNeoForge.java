/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.gametest.EntityCapabilityTests;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import java.util.function.Predicate;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "entity_capability")
public final class EntityCapabilityTestsNeoForge {
    private static final Predicate<Robot> HAS_ITEM_HANDLER = robot ->
        robot.getCapability(Capabilities.Item.ENTITY_AUTOMATION, Direction.DOWN) != null;
    private static final Predicate<Robot> HAS_ENERGY_STORAGE = robot ->
        robot.getCapability(Capabilities.Energy.ENTITY, Direction.DOWN) != null;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void robotProvidesTerminalUsers(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesTerminalUsers(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void robotProvidesInventoryAndEnergy(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesInventoryAndEnergy(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void robotIsVisibleToPlatformCapabilities(final GameTestHelper helper) {
        EntityCapabilityTests.robotIsVisibleToPlatformCapabilities(helper, HAS_ITEM_HANDLER, HAS_ENERGY_STORAGE);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignEntityInventoryIsReachable(final GameTestHelper helper) {
        EntityCapabilityTests.foreignEntityInventoryIsReachable(helper);
    }

    // --------------------------------------------------------------------- //

    private EntityCapabilityTestsNeoForge() {
    }
}

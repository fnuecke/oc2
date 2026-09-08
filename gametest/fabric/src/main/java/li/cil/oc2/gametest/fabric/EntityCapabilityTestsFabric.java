/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.api.fabric.EnergyStorage;
import li.cil.oc2.api.fabric.ItemStorage;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.gametest.EntityCapabilityTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.Predicate;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class EntityCapabilityTestsFabric {
    private static final Predicate<Robot> HAS_ITEM_HANDLER = robot -> ItemStorage.ENTITY.find(robot, null) != null;
    private static final Predicate<Robot> HAS_ENERGY_STORAGE = robot -> EnergyStorage.ENTITY.find(robot, null) != null;

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public void robotProvidesTerminalUsers(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesTerminalUsers(helper);
    }

    @GameTest(template = TEMPLATE)
    public void robotProvidesInventoryAndEnergy(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesInventoryAndEnergy(helper);
    }

    @GameTest(template = TEMPLATE)
    public void robotIsVisibleToPlatformCapabilities(final GameTestHelper helper) {
        EntityCapabilityTests.robotIsVisibleToPlatformCapabilities(helper, HAS_ITEM_HANDLER, HAS_ENERGY_STORAGE);
    }

    @GameTest(template = TEMPLATE)
    public void foreignEntityInventoryIsReachable(final GameTestHelper helper) {
        EntityCapabilityTests.foreignEntityInventoryIsReachable(helper);
    }
}

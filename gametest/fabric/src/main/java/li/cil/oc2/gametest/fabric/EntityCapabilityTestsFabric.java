/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.api.fabric.EnergyStorage;
import li.cil.oc2.api.fabric.ItemStorage;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.gametest.EntityCapabilityTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import java.util.function.Predicate;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class EntityCapabilityTestsFabric {
    private static final Predicate<Robot> HAS_ITEM_HANDLER = robot -> ItemStorage.ENTITY.find(robot, null) != null;
    private static final Predicate<Robot> HAS_ENERGY_STORAGE = robot -> EnergyStorage.ENTITY.find(robot, null) != null;

    // --------------------------------------------------------------------- //

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void robotProvidesTerminalUsers(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesTerminalUsers(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void robotProvidesInventoryAndEnergy(final GameTestHelper helper) {
        EntityCapabilityTests.robotProvidesInventoryAndEnergy(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void robotIsVisibleToPlatformCapabilities(final GameTestHelper helper) {
        EntityCapabilityTests.robotIsVisibleToPlatformCapabilities(helper, HAS_ITEM_HANDLER, HAS_ENERGY_STORAGE);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignEntityInventoryIsReachable(final GameTestHelper helper) {
        EntityCapabilityTests.foreignEntityInventoryIsReachable(helper);
    }
}

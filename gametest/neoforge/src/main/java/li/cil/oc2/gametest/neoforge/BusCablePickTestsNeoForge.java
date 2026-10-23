/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.BusCablePickTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "bus_cable_pick")
public final class BusCablePickTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void picksInterfaceWhenLookingAtInterface(final GameTestHelper helper) {
        BusCablePickTests.picksInterfaceWhenLookingAtInterface(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void picksCableWhenLookingAtCable(final GameTestHelper helper) {
        BusCablePickTests.picksCableWhenLookingAtCable(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void picksFacade(final GameTestHelper helper) {
        BusCablePickTests.picksFacade(helper);
    }

    // --------------------------------------------------------------------- //

    private BusCablePickTestsNeoForge() {
    }
}

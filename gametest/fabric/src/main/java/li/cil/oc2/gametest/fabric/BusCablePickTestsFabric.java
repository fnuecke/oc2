/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.BusCablePickTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class BusCablePickTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void picksInterfaceWhenLookingAtInterface(final GameTestHelper helper) {
        BusCablePickTests.picksInterfaceWhenLookingAtInterface(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void picksCableWhenLookingAtCable(final GameTestHelper helper) {
        BusCablePickTests.picksCableWhenLookingAtCable(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void picksFacade(final GameTestHelper helper) {
        BusCablePickTests.picksFacade(helper);
    }
}

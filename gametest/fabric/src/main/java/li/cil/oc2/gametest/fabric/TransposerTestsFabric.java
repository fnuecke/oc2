/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.TransposerTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class TransposerTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void transposerJoinsTheBus(final GameTestHelper helper) {
        TransposerTests.transposerJoinsTheBus(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void transposerMovesItemsBetweenInventories(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsBetweenInventories(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void transposerMovesItemsViaMidLevelApi(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsViaMidLevelApi(helper);
    }
}

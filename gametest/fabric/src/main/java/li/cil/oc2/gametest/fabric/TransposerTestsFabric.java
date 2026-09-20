/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.TransposerTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class TransposerTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void transposerJoinsTheBus(final GameTestHelper helper) {
        TransposerTests.transposerJoinsTheBus(helper);
    }

    @GameTest(template = TEMPLATE)
    public void transposerMovesItemsBetweenInventories(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsBetweenInventories(helper);
    }

    @GameTest(template = TEMPLATE)
    public void transposerMovesItemsViaMidLevelApi(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsViaMidLevelApi(helper);
    }
}

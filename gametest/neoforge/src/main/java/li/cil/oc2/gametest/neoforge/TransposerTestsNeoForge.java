/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.TransposerTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "transposer")
public final class TransposerTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void transposerJoinsTheBus(final GameTestHelper helper) {
        TransposerTests.transposerJoinsTheBus(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void transposerMovesItemsBetweenInventories(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsBetweenInventories(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void transposerMovesItemsViaMidLevelApi(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsViaMidLevelApi(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void transposerMovesDeviceItemBetweenComputers(final GameTestHelper helper) {
        TransposerTests.transposerMovesDeviceItemBetweenComputers(helper);
    }

    // --------------------------------------------------------------------- //

    private TransposerTestsNeoForge() {
    }
}

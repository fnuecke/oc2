/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.TransposerTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class TransposerTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void transposerJoinsTheBus(final GameTestHelper helper) {
        TransposerTests.transposerJoinsTheBus(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void transposerMovesItemsBetweenInventories(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsBetweenInventories(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void transposerMovesItemsViaMidLevelApi(final GameTestHelper helper) {
        TransposerTests.transposerMovesItemsViaMidLevelApi(helper);
    }
}

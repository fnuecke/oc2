/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.RPCTypeAdapterTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RPCTypeAdapterTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void directionAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.directionAdapterIsApplied(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void itemStackAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.itemStackAdapterIsApplied(helper);
    }

    // --------------------------------------------------------------------- //

    private RPCTypeAdapterTestsNeoForge() {
    }
}

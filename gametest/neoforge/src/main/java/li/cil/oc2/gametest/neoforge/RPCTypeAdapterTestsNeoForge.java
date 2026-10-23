/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.RPCTypeAdapterTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "rpc_type_adapter")
public final class RPCTypeAdapterTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void directionAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.directionAdapterIsApplied(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void itemStackAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.itemStackAdapterIsApplied(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void itemStackCarriesFilteredComponents(final GameTestHelper helper) {
        RPCTypeAdapterTests.itemStackCarriesFilteredComponents(helper);
    }

    // --------------------------------------------------------------------- //

    private RPCTypeAdapterTestsNeoForge() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.RPCTypeAdapterTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class RPCTypeAdapterTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void directionAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.directionAdapterIsApplied(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void itemStackAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.itemStackAdapterIsApplied(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void itemStackCarriesFilteredComponents(final GameTestHelper helper) {
        RPCTypeAdapterTests.itemStackCarriesFilteredComponents(helper);
    }
}

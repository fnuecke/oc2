/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.RPCTypeAdapterTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.FabricTestSupport.TEMPLATE;

public final class RPCTypeAdapterTestsFabric {
    @GameTest(template = TEMPLATE)
    public void directionAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.directionAdapterIsApplied(helper);
    }

    @GameTest(template = TEMPLATE)
    public void itemStackAdapterIsApplied(final GameTestHelper helper) {
        RPCTypeAdapterTests.itemStackAdapterIsApplied(helper);
    }
}

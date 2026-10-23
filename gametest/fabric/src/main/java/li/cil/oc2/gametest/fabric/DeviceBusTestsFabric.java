/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.DeviceBusTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class DeviceBusTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 800)
    public void busRediscoversReplacedNeighbor(final GameTestHelper helper) {
        DeviceBusTests.busRediscoversReplacedNeighbor(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 600)
    public void busDropsNeighborWithoutBlockUpdate(final GameTestHelper helper) {
        DeviceBusTests.busDropsNeighborWithoutBlockUpdate(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 900)
    public void busReachesAcrossChunkBoundary(final GameTestHelper helper) {
        DeviceBusTests.busReachesAcrossChunkBoundary(helper);
    }
}

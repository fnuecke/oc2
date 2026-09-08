/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.DeviceBusTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class DeviceBusTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void busTracksNeighborLifecycle(final GameTestHelper helper) {
        DeviceBusTests.busTracksNeighborLifecycle(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public void busRediscoversReplacedNeighbor(final GameTestHelper helper) {
        DeviceBusTests.busRediscoversReplacedNeighbor(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public void busNoticesNeighborCapabilityInvalidatedWithoutBlockUpdate(final GameTestHelper helper) {
        DeviceBusTests.busNoticesNeighborCapabilityInvalidatedWithoutBlockUpdate(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public void busReachesAcrossChunkBoundary(final GameTestHelper helper) {
        DeviceBusTests.busReachesAcrossChunkBoundary(helper);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.DeviceBusTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "device_bus")
public final class DeviceBusTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = 800)
    @TestHolder
    public static void busRediscoversReplacedNeighbor(final GameTestHelper helper) {
        DeviceBusTests.busRediscoversReplacedNeighbor(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void busDropsNeighborWithoutBlockUpdate(final GameTestHelper helper) {
        DeviceBusTests.busDropsNeighborWithoutBlockUpdate(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 900)
    @TestHolder
    public static void busReachesAcrossChunkBoundary(final GameTestHelper helper) {
        DeviceBusTests.busReachesAcrossChunkBoundary(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = 600)
    @TestHolder
    public static void blockDeviceMountsFromOfferingFaceOnly(final GameTestHelper helper) {
        DeviceBusTests.blockDeviceMountsFromOfferingFaceOnly(helper);
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTestsNeoForge() {
    }
}

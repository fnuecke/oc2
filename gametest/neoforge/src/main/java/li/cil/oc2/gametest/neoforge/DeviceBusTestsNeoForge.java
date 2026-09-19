/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.DeviceBusTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeviceBusTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void busTracksNeighborLifecycle(final GameTestHelper helper) {
        DeviceBusTests.busTracksNeighborLifecycle(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void busRediscoversReplacedNeighbor(final GameTestHelper helper) {
        DeviceBusTests.busRediscoversReplacedNeighbor(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void busDropsNeighborWithoutBlockUpdate(final GameTestHelper helper) {
        DeviceBusTests.busDropsNeighborWithoutBlockUpdate(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void busReachesAcrossChunkBoundary(final GameTestHelper helper) {
        DeviceBusTests.busReachesAcrossChunkBoundary(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void blockDeviceMountsFromOfferingFaceOnly(final GameTestHelper helper) {
        DeviceBusTests.blockDeviceMountsFromOfferingFaceOnly(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void noteBlockJoinsAndLeavesTheBus(final GameTestHelper helper) {
        DeviceBusTests.noteBlockJoinsAndLeavesTheBus(helper);
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTestsNeoForge() {
    }
}

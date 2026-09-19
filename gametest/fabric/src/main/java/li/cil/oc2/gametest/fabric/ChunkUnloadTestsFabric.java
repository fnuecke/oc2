/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.ChunkUnloadTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;
import static li.cil.oc2.gametest.util.TestSupport.BOOT_TIMEOUT_TICKS;

public final class ChunkUnloadTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_machine")
    public void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.runningMachineSurvivesChunkUnload(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_writes")
    public void diskSurvivesUnloadsUnderWrites(final GameTestHelper helper) {
        ChunkUnloadTests.diskSurvivesUnloadsUnderWrites(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_robot")
    public void robotSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.robotSurvivesChunkUnload(helper);
    }
}

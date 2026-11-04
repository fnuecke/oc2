/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.ChunkUnloadTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.REAL_TIME_TIMEOUT_TICKS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class ChunkUnloadTestsFabric {
    @GameTest(structure = TEMPLATE_ID, environment = "oc2_gametest:unload_machine", maxTicks = REAL_TIME_TIMEOUT_TICKS)
    public void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.runningMachineSurvivesChunkUnload(helper);
    }

    @GameTest(structure = TEMPLATE_ID, environment = "oc2_gametest:unload_writes", maxTicks = REAL_TIME_TIMEOUT_TICKS)
    public void diskSurvivesUnloadsUnderWrites(final GameTestHelper helper) {
        ChunkUnloadTests.diskSurvivesUnloadsUnderWrites(helper);
    }

    @GameTest(structure = TEMPLATE_ID, environment = "oc2_gametest:unload_robot", maxTicks = REAL_TIME_TIMEOUT_TICKS)
    public void robotSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.robotSurvivesChunkUnload(helper);
    }
}

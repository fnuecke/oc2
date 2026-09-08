/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.ChunkUnloadTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class ChunkUnloadTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 1600)
    public void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.runningMachineSurvivesChunkUnload(helper);
    }
}

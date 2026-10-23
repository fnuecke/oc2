/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.ChunkUnloadTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.BOOT_TIMEOUT_TICKS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "chunk_unload")
public final class ChunkUnloadTestsNeoForge {
    @GameTest(template = TEMPLATE_ID, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_machine")
    @TestHolder
    public static void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.runningMachineSurvivesChunkUnload(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_writes")
    @TestHolder
    public static void diskSurvivesUnloadsUnderWrites(final GameTestHelper helper) {
        ChunkUnloadTests.diskSurvivesUnloadsUnderWrites(helper);
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_robot")
    @TestHolder
    public static void robotSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.robotSurvivesChunkUnload(helper);
    }

    // --------------------------------------------------------------------- //

    private ChunkUnloadTestsNeoForge() {
    }
}

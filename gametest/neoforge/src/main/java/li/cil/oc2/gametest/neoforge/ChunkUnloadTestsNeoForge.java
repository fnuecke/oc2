/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.ChunkUnloadTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.BOOT_TIMEOUT_TICKS;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class ChunkUnloadTestsNeoForge {
    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_machine")
    public static void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.runningMachineSurvivesChunkUnload(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_writes")
    public static void diskSurvivesUnloadsUnderWrites(final GameTestHelper helper) {
        ChunkUnloadTests.diskSurvivesUnloadsUnderWrites(helper);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = "oc2_unload_robot")
    public static void robotSurvivesChunkUnload(final GameTestHelper helper) {
        ChunkUnloadTests.robotSurvivesChunkUnload(helper);
    }

    // --------------------------------------------------------------------- //

    private ChunkUnloadTestsNeoForge() {
    }
}

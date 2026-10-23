/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.WrenchTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "wrench")
public final class WrenchTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void rotatesOnTopFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnTopFace(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void rotatesOnBottomFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnBottomFace(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void doesNotRotateOnSideFace(final GameTestHelper helper) {
        WrenchTests.doesNotRotateOnSideFace(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void sneakingDoesNotBypassBlockInteraction(final GameTestHelper helper) {
        WrenchTests.sneakingDoesNotBypassBlockInteraction(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void sneakingBreaksWrenchBreakableBlock(final GameTestHelper helper) {
        WrenchTests.sneakingBreaksWrenchBreakableBlock(helper);
    }

    // --------------------------------------------------------------------- //

    private WrenchTestsNeoForge() {
    }
}

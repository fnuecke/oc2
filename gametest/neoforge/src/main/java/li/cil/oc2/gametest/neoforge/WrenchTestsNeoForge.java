/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.WrenchTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class WrenchTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void rotatesOnTopFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnTopFace(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void rotatesOnBottomFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnBottomFace(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void doesNotRotateOnSideFace(final GameTestHelper helper) {
        WrenchTests.doesNotRotateOnSideFace(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingDoesNotBypassBlockInteraction(final GameTestHelper helper) {
        WrenchTests.sneakingDoesNotBypassBlockInteraction(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void sneakingBreaksWrenchBreakableBlock(final GameTestHelper helper) {
        WrenchTests.sneakingBreaksWrenchBreakableBlock(helper);
    }

    private WrenchTestsNeoForge() {
    }
}

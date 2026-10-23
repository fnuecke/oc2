/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.WrenchTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class WrenchTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void rotatesOnTopFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnTopFace(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void rotatesOnBottomFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnBottomFace(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void doesNotRotateOnSideFace(final GameTestHelper helper) {
        WrenchTests.doesNotRotateOnSideFace(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void sneakingDoesNotBypassBlockInteraction(final GameTestHelper helper) {
        WrenchTests.sneakingDoesNotBypassBlockInteraction(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void sneakingBreaksWrenchBreakableBlock(final GameTestHelper helper) {
        WrenchTests.sneakingBreaksWrenchBreakableBlock(helper);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.WrenchTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.FabricTestSupport.TEMPLATE;

public final class WrenchTestsFabric {
    @GameTest(template = TEMPLATE)
    public void rotatesOnTopFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnTopFace(helper);
    }

    @GameTest(template = TEMPLATE)
    public void rotatesOnBottomFace(final GameTestHelper helper) {
        WrenchTests.rotatesOnBottomFace(helper);
    }

    @GameTest(template = TEMPLATE)
    public void doesNotRotateOnSideFace(final GameTestHelper helper) {
        WrenchTests.doesNotRotateOnSideFace(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sneakingDoesNotBypassBlockInteraction(final GameTestHelper helper) {
        WrenchTests.sneakingDoesNotBypassBlockInteraction(helper);
    }

    @GameTest(template = TEMPLATE)
    public void sneakingBreaksWrenchBreakableBlock(final GameTestHelper helper) {
        WrenchTests.sneakingBreaksWrenchBreakableBlock(helper);
    }
}

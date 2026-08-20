/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.BlockOperationsModuleTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.FabricTestSupport.TEMPLATE;

public final class BlockOperationsModuleTestsFabric {
    @GameTest(template = TEMPLATE)
    public void excavatesWithACorrectTool(final GameTestHelper helper) {
        BlockOperationsModuleTests.excavatesWithACorrectTool(helper);
    }

    @GameTest(template = TEMPLATE)
    public void placesFromTheSelectedSlot(final GameTestHelper helper) {
        BlockOperationsModuleTests.placesFromTheSelectedSlot(helper);
    }
}

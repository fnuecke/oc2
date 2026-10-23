/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.RobotCrushTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class RobotCrushTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 120)
    public void crushesAnOrdinaryBlock(final GameTestHelper helper) {
        RobotCrushTests.crushesAnOrdinaryBlock(helper);
    }
}

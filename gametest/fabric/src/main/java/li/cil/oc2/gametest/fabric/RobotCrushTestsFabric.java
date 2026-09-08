/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.RobotCrushTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class RobotCrushTestsFabric {
    @GameTest(template = TEMPLATE, timeoutTicks = 120)
    public void crushesAnOrdinaryBlock(final GameTestHelper helper) {
        RobotCrushTests.crushesAnOrdinaryBlock(helper);
    }
}

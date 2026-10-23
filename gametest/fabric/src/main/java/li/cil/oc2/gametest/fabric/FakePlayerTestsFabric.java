/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.FakePlayerTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class FakePlayerTestsFabric {
    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void fakePlayerIsRecognisedAsFake(final GameTestHelper helper) {
        FakePlayerTests.fakePlayerIsRecognisedAsFake(helper);
    }
}

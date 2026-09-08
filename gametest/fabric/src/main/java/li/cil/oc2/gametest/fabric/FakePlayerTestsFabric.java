/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.FakePlayerTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;

public final class FakePlayerTestsFabric {
    @GameTest(template = TEMPLATE)
    public void fakePlayerIsRecognisedAsFake(final GameTestHelper helper) {
        FakePlayerTests.fakePlayerIsRecognisedAsFake(helper);
    }
}

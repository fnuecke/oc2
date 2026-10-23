/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.FakePlayerTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "fake_player")
public final class FakePlayerTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void fakePlayerIsRecognisedAsFake(final GameTestHelper helper) {
        FakePlayerTests.fakePlayerIsRecognisedAsFake(helper);
    }

    // --------------------------------------------------------------------- //

    private FakePlayerTestsNeoForge() {
    }
}

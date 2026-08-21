/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.FakePlayerTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class FakePlayerTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void fakePlayerIsRecognisedAsFake(final GameTestHelper helper) {
        FakePlayerTests.fakePlayerIsRecognisedAsFake(helper);
    }

    // --------------------------------------------------------------------- //

    private FakePlayerTestsNeoForge() {
    }
}

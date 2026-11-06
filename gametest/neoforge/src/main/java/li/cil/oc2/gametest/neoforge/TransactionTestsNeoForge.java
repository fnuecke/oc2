/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.TransactionTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "transaction")
public final class TransactionTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void closingRevertsNestedChanges(final GameTestHelper helper) {
        TransactionTests.closingRevertsNestedChanges(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void commitHookRunsOncePerRootCommit(final GameTestHelper helper) {
        TransactionTests.commitHookRunsOncePerRootCommit(helper);
    }

    // --------------------------------------------------------------------- //

    private TransactionTestsNeoForge() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.TransactionTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class TransactionTestsFabric {
    @GameTest(structure = TEMPLATE_ID)
    public void closingRevertsNestedChanges(final GameTestHelper helper) {
        TransactionTests.closingRevertsNestedChanges(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void commitHookRunsOncePerRootCommit(final GameTestHelper helper) {
        TransactionTests.commitHookRunsOncePerRootCommit(helper);
    }
}

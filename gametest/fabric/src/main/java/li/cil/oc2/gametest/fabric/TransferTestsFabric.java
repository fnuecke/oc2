/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.TransferTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class TransferTestsFabric {
    @GameTest(structure = TEMPLATE_ID)
    public void itemTransferMovesWhatFits(final GameTestHelper helper) {
        TransferTests.itemTransferMovesWhatFits(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void itemTransferWithRemainderChangesNothing(final GameTestHelper helper) {
        TransferTests.itemTransferWithRemainderChangesNothing(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void worldItemHandlerAppliesOnCommit(final GameTestHelper helper) {
        TransferTests.worldItemHandlerAppliesOnCommit(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void worldFluidHandlerAppliesOnCommit(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerAppliesOnCommit(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void fluidTransferIntoRejectingContainerKeepsFluid(final GameTestHelper helper) {
        TransferTests.fluidTransferIntoRejectingContainerKeepsFluid(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void fluidTransferWithRemainderChangesNothing(final GameTestHelper helper) {
        TransferTests.fluidTransferWithRemainderChangesNothing(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void worldFluidHandlerReplacesDrainedFluid(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerReplacesDrainedFluid(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void worldFluidHandlerCancelsFillByDrain(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerCancelsFillByDrain(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void worldFluidHandlerRefillsWaterloggedBlockWithWaterOnly(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerRefillsWaterloggedBlockWithWaterOnly(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void craftingWithRemainderThatDoesNotFitAborts(final GameTestHelper helper) {
        TransferTests.craftingWithRemainderThatDoesNotFitAborts(helper);
    }

    @GameTest(structure = TEMPLATE_ID)
    public void diskDriveSwapEjectsOldMedium(final GameTestHelper helper) {
        TransferTests.diskDriveSwapEjectsOldMedium(helper);
    }
}

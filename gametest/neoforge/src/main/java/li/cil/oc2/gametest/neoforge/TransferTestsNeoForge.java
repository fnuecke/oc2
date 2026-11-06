/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.TransferTests;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "transfer")
public final class TransferTestsNeoForge {
    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void itemTransferMovesWhatFits(final GameTestHelper helper) {
        TransferTests.itemTransferMovesWhatFits(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void itemTransferWithRemainderChangesNothing(final GameTestHelper helper) {
        TransferTests.itemTransferWithRemainderChangesNothing(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void worldItemHandlerAppliesOnCommit(final GameTestHelper helper) {
        TransferTests.worldItemHandlerAppliesOnCommit(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void worldFluidHandlerAppliesOnCommit(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerAppliesOnCommit(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void fluidTransferIntoRejectingContainerKeepsFluid(final GameTestHelper helper) {
        TransferTests.fluidTransferIntoRejectingContainerKeepsFluid(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void fluidTransferWithRemainderChangesNothing(final GameTestHelper helper) {
        TransferTests.fluidTransferWithRemainderChangesNothing(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void worldFluidHandlerReplacesDrainedFluid(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerReplacesDrainedFluid(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void worldFluidHandlerCancelsFillByDrain(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerCancelsFillByDrain(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void worldFluidHandlerRefillsWaterloggedBlockWithWaterOnly(final GameTestHelper helper) {
        TransferTests.worldFluidHandlerRefillsWaterloggedBlockWithWaterOnly(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void craftingWithRemainderThatDoesNotFitAborts(final GameTestHelper helper) {
        TransferTests.craftingWithRemainderThatDoesNotFitAborts(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void diskDriveSwapEjectsOldMedium(final GameTestHelper helper) {
        TransferTests.diskDriveSwapEjectsOldMedium(helper);
    }

    // --------------------------------------------------------------------- //

    private TransferTestsNeoForge() {
    }
}

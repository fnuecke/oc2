/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.gametest.CapabilityAdapterTests;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.CapabilityAdapterTests.ENERGY;
import static li.cil.oc2.gametest.CapabilityAdapterTests.ITEMS;
import static li.cil.oc2.gametest.fabric.util.FabricTestSupport.TEMPLATE;
import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;

public final class CapabilityAdapterTestsFabric {
    private static final CapabilityAdapterTests.EnergyOperation ABORTED_INSERT = (helper, storage, amount) -> {
        final team.reborn.energy.api.EnergyStorage platform = platformEnergy(helper);
        try (Transaction transaction = Transaction.openOuter()) {
            return platform.insert(amount, transaction);
        }
    };
    private static final CapabilityAdapterTests.EnergyOperation ABORTED_EXTRACT = (helper, storage, amount) -> {
        final team.reborn.energy.api.EnergyStorage platform = platformEnergy(helper);
        try (Transaction transaction = Transaction.openOuter()) {
            return platform.extract(amount, transaction);
        }
    };
    private static final CapabilityAdapterTests.ItemOperation ABORTED_ITEM_INSERT = (helper, handler, slot, stack) -> {
        final Storage<ItemVariant> platform = platformItems(helper);
        try (Transaction transaction = Transaction.openOuter()) {
            final long inserted = platform.insert(ItemVariant.of(stack), stack.getCount(), transaction);
            return stack.copyWithCount(stack.getCount() - (int) inserted);
        }
    };

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public void simulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public void committedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public void simulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public void committedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public void insertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public void extractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public void abortedInsertLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedInsertLeavesStorageUnchanged(helper, ENERGY, ABORTED_INSERT);
    }

    @GameTest(template = TEMPLATE)
    public void abortedExtractLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedExtractLeavesStorageUnchanged(helper, ENERGY, ABORTED_EXTRACT);
    }

    @GameTest(template = TEMPLATE)
    public void simulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public void committedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public void itemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public void abortedItemInsertLeavesHandlerUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedItemInsertLeavesHandlerUnchanged(helper, ITEMS, ABORTED_ITEM_INSERT);
    }

    @GameTest(template = TEMPLATE)
    public void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerItemSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemSimulatedInsertDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerItemInsertAndExtract(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertAndExtract(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertReturnsRemainder(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerFluidSimulatedFillDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidSimulatedFillDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerFluidFillReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillReplacesItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerFluidFillSplitsStack(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillSplitsStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidDrainReplacesItem(helper);
    }

    // --------------------------------------------------------------------- //

    private static team.reborn.energy.api.EnergyStorage platformEnergy(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final team.reborn.energy.api.EnergyStorage storage =
            team.reborn.energy.api.EnergyStorage.SIDED.find(helper.getLevel(), pos, null);
        if (storage == null) {
            throw new GameTestAssertException("charger is not visible to the Team Reborn energy lookup");
        }
        return storage;
    }

    private static Storage<ItemVariant> platformItems(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), pos, null);
        if (storage == null) {
            throw new GameTestAssertException("computer is not visible to the item transfer lookup");
        }
        return storage;
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fabric;

import li.cil.oc2.common.capabilities.fabric.FabricCapabilityAdapters;
import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.gametest.CapabilityAdapterTests;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.function.Function;

import static li.cil.oc2.gametest.CapabilityAdapterTests.ENERGY;
import static li.cil.oc2.gametest.CapabilityAdapterTests.FOREIGN_ITEMS;
import static li.cil.oc2.gametest.CapabilityAdapterTests.ITEMS;
import static li.cil.oc2.gametest.CapabilityAdapterTests.LARGE_SLOT_AMOUNT;
import static li.cil.oc2.gametest.CapabilityAdapterTests.LARGE_SLOT_ITEM;
import static li.cil.oc2.gametest.CapabilityAdapterTests.SIMULATED_EXTRACT;
import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

public final class CapabilityAdapterTestsFabric {
    private static final int FOREIGN_ENERGY_CAPACITY = 1000;
    private static final Function<GameTestHelper, EnergyHandler> FOREIGN_ENERGY = helper ->
        FabricCapabilityAdapters.energy(new SimpleEnergyStorage(FOREIGN_ENERGY_CAPACITY, FOREIGN_ENERGY_CAPACITY, FOREIGN_ENERGY_CAPACITY));
    private static final Function<GameTestHelper, ItemHandler> LARGE_SLOT = helper -> {
        final SingleVariantStorage<ItemVariant> storage = new SingleVariantStorage<>() {
            @Override
            protected ItemVariant getBlankVariant() {
                return ItemVariant.blank();
            }

            @Override
            protected long getCapacity(final ItemVariant variant) {
                return LARGE_SLOT_AMOUNT;
            }
        };
        storage.variant = ItemVariant.of(LARGE_SLOT_ITEM);
        storage.amount = LARGE_SLOT_AMOUNT;
        return FabricCapabilityAdapters.items(storage);
    };
    private static final CapabilityAdapterTests.ItemOperation ABORTED_ITEM_EXTRACT = (helper, handler, slot, stack) -> {
        final SlottedStorage<ItemVariant> platform = (SlottedStorage<ItemVariant>) platformItems(helper);
        try (Transaction transaction = Transaction.openOuter()) {
            return stack.copyWithCount((int) platform.getSlot(slot).extract(ItemVariant.of(stack), stack.getCount(), transaction));
        }
    };
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
    private static final CapabilityAdapterTests.ItemMove ITEM_MOVE = (helper, slot, stack, commit) -> {
        final SlottedStorage<ItemVariant> source = (SlottedStorage<ItemVariant>) platformItems(helper);
        final SlottedStorage<ItemVariant> target = (SlottedStorage<ItemVariant>) platformItems(helper);
        final ItemVariant resource = ItemVariant.of(stack);
        try (Transaction transaction = Transaction.openOuter()) {
            final long extracted = source.getSlot(slot).extract(resource, stack.getCount(), transaction);
            final long inserted = target.getSlot(slot).insert(resource, extracted, transaction);
            if (commit) {
                transaction.commit();
            }
            return (int) inserted;
        }
    };
    private static final CapabilityAdapterTests.FluidMove FLUID_MOVE = (handler, stack, commit) -> {
        final Storage<FluidVariant> source = FabricCapabilityAdapters.toFabric(handler);
        final Storage<FluidVariant> target = FabricCapabilityAdapters.toFabric(handler);
        final FluidVariant resource = FluidVariant.of(stack.fluid(), stack.components());
        final long dropletsPerMillibucket = FluidConstants.BUCKET / FluidHandler.BUCKET;
        try (Transaction transaction = Transaction.openOuter()) {
            final long extracted = source.extract(resource, stack.amount() * dropletsPerMillibucket, transaction);
            final long inserted = target.insert(resource, extracted, transaction);
            if (commit) {
                transaction.commit();
            }
            return (int) (inserted / dropletsPerMillibucket);
        }
    };

    // --------------------------------------------------------------------- //

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void simulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void committedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void simulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void committedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void insertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void extractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, FOREIGN_ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignCommittedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, FOREIGN_ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignSimulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, FOREIGN_ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignCommittedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, FOREIGN_ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignInsertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, FOREIGN_ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignExtractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, FOREIGN_ENERGY);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void abortedInsertLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedInsertLeavesStorageUnchanged(helper, ENERGY, ABORTED_INSERT);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void abortedExtractLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedExtractLeavesStorageUnchanged(helper, ENERGY, ABORTED_EXTRACT);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void simulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, ITEMS);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void committedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, ITEMS);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void itemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, ITEMS);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignSimulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, FOREIGN_ITEMS);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignCommittedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, FOREIGN_ITEMS);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void foreignItemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, FOREIGN_ITEMS);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void abortedItemInsertLeavesHandlerUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedItemInsertLeavesHandlerUnchanged(helper, ITEMS, ABORTED_ITEM_INSERT);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void itemMoveBetweenAdaptersLeavesSlotUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.itemMoveBetweenAdaptersLeavesSlotUnchanged(helper, ITEM_MOVE);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void fluidMoveBetweenAdaptersLeavesTankUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.fluidMoveBetweenAdaptersLeavesTankUnchanged(helper, FLUID_MOVE);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void simulatedExtractLeavesDeviceSlotUntouched(final GameTestHelper helper) {
        CapabilityAdapterTests.extractLeavesDeviceSlotUntouched(helper, SIMULATED_EXTRACT);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void abortedExtractLeavesDeviceSlotUntouched(final GameTestHelper helper) {
        CapabilityAdapterTests.extractLeavesDeviceSlotUntouched(helper, ABORTED_ITEM_EXTRACT);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void extractFromLargeSlotYieldsAtMostAStack(final GameTestHelper helper) {
        CapabilityAdapterTests.extractFromLargeSlotYieldsAtMostAStack(helper, LARGE_SLOT);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToInventory(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void slotCapabilityWritesToForeignInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToForeignInventory(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void slotCapabilityRespectsInsertRestrictions(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityRespectsInsertRestrictions(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void stackCapabilityWritesToStack(final GameTestHelper helper) {
        CapabilityAdapterTests.stackCapabilityWritesToStack(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void stackCapabilityKeepsItem(final GameTestHelper helper) {
        CapabilityAdapterTests.stackCapabilityKeepsItem(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerItemSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemSimulatedInsertDoesNotMutate(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerItemInsertAndExtract(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertAndExtract(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertReturnsRemainder(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerFluidSimulatedFillDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidSimulatedFillDoesNotMutate(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerFluidFillReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillReplacesItem(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerFluidFillSplitsStack(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillSplitsStack(helper);
    }

    @GameTest(structure = TEMPLATE_ID, maxTicks = 100)
    public void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidDrainReplacesItem(helper);
    }

    // --------------------------------------------------------------------- //

    private static team.reborn.energy.api.EnergyStorage platformEnergy(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final team.reborn.energy.api.EnergyStorage storage =
            team.reborn.energy.api.EnergyStorage.SIDED.find(helper.getLevel(), pos, null);
        if (storage == null) {
            throw new GameTestAssertException(Component.literal("charger is not visible to the Team Reborn energy lookup"), 0);
        }
        return storage;
    }

    private static Storage<ItemVariant> platformItems(final GameTestHelper helper) {
        final BlockPos pos = helper.absolutePos(DEVICE_POS);
        final Storage<ItemVariant> storage = ItemStorage.SIDED.find(helper.getLevel(), pos, null);
        if (storage == null) {
            throw new GameTestAssertException(Component.literal("computer is not visible to the item transfer lookup"), 0);
        }
        return storage;
    }
}

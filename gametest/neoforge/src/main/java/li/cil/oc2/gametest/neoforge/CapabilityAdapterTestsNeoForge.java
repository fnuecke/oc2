/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.capabilities.neoforge.NeoForgeCapabilityAdapters;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.gametest.CapabilityAdapterTests;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.energy.SimpleEnergyHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import java.util.function.Function;

import static li.cil.oc2.gametest.CapabilityAdapterTests.ENERGY;
import static li.cil.oc2.gametest.CapabilityAdapterTests.FOREIGN_ITEMS;
import static li.cil.oc2.gametest.CapabilityAdapterTests.ITEMS;
import static li.cil.oc2.gametest.CapabilityAdapterTests.LARGE_SLOT_AMOUNT;
import static li.cil.oc2.gametest.CapabilityAdapterTests.LARGE_SLOT_ITEM;
import static li.cil.oc2.gametest.CapabilityAdapterTests.SIMULATED_EXTRACT;
import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE_ID;

@ForEachTest(groups = "capability_adapter")
public final class CapabilityAdapterTestsNeoForge {
    private static final int FOREIGN_ENERGY_CAPACITY = 1000;
    private static final Function<GameTestHelper, li.cil.oc2.common.energy.EnergyHandler> FOREIGN_ENERGY = helper ->
        NeoForgeCapabilityAdapters.energy(new SimpleEnergyHandler(FOREIGN_ENERGY_CAPACITY));
    private static final Function<GameTestHelper, ItemHandler> LARGE_SLOT = helper -> {
        final ItemStacksResourceHandler handler = new ItemStacksResourceHandler(1) {
            @Override
            protected int getCapacity(final int index, final ItemResource resource) {
                return LARGE_SLOT_AMOUNT;
            }
        };
        handler.set(0, ItemResource.of(LARGE_SLOT_ITEM), LARGE_SLOT_AMOUNT);
        return NeoForgeCapabilityAdapters.items(handler);
    };
    private static final CapabilityAdapterTests.ItemOperation ABORTED_ITEM_EXTRACT = (helper, handler, slot, stack) -> {
        final ResourceHandler<ItemResource> platform = platformItems(helper);
        try (Transaction transaction = Transaction.openRoot()) {
            return stack.copyWithCount(platform.extract(slot, ItemResource.of(stack), stack.getCount(), transaction));
        }
    };
    private static final CapabilityAdapterTests.EnergyOperation ABORTED_INSERT = (helper, storage, amount) -> {
        final EnergyHandler platform = platformEnergy(helper);
        try (Transaction transaction = Transaction.openRoot()) {
            return platform.insert((int) amount, transaction);
        }
    };
    private static final CapabilityAdapterTests.EnergyOperation ABORTED_EXTRACT = (helper, storage, amount) -> {
        final EnergyHandler platform = platformEnergy(helper);
        try (Transaction transaction = Transaction.openRoot()) {
            return platform.extract((int) amount, transaction);
        }
    };
    private static final CapabilityAdapterTests.ItemOperation ABORTED_ITEM_INSERT = (helper, handler, slot, stack) -> {
        final ResourceHandler<ItemResource> platform = platformItems(helper);
        try (Transaction transaction = Transaction.openRoot()) {
            final int inserted = platform.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
            return stack.copyWithCount(stack.getCount() - inserted);
        }
    };
    private static final CapabilityAdapterTests.ItemMove ITEM_MOVE = (helper, slot, stack, commit) -> {
        final ResourceHandler<ItemResource> source = platformItems(helper);
        final ResourceHandler<ItemResource> target = platformItems(helper);
        final ItemResource resource = ItemResource.of(stack);
        try (Transaction transaction = Transaction.openRoot()) {
            final int extracted = source.extract(slot, resource, stack.getCount(), transaction);
            final int inserted = target.insert(slot, resource, extracted, transaction);
            if (commit) {
                transaction.commit();
            }
            return inserted;
        }
    };
    private static final CapabilityAdapterTests.FluidMove FLUID_MOVE = (handler, stack, commit) -> {
        final ResourceHandler<FluidResource> source = NeoForgeCapabilityAdapters.toNeoForge(handler);
        final ResourceHandler<FluidResource> target = NeoForgeCapabilityAdapters.toNeoForge(handler);
        final FluidResource resource = FluidResource.of(stack.fluid(), stack.components());
        try (Transaction transaction = Transaction.openRoot()) {
            final int extracted = source.extract(resource, stack.amount(), transaction);
            final int inserted = target.insert(resource, extracted, transaction);
            if (commit) {
                transaction.commit();
            }
            return inserted;
        }
    };

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void simulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void committedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void simulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void committedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void insertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void extractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, FOREIGN_ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignCommittedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, FOREIGN_ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignSimulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, FOREIGN_ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignCommittedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, FOREIGN_ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignInsertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, FOREIGN_ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignExtractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, FOREIGN_ENERGY);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void abortedInsertLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedInsertLeavesStorageUnchanged(helper, ENERGY, ABORTED_INSERT);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void abortedExtractLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedExtractLeavesStorageUnchanged(helper, ENERGY, ABORTED_EXTRACT);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void simulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void committedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void itemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignSimulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, FOREIGN_ITEMS);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignCommittedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, FOREIGN_ITEMS);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void foreignItemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, FOREIGN_ITEMS);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void abortedItemInsertLeavesHandlerUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedItemInsertLeavesHandlerUnchanged(helper, ITEMS, ABORTED_ITEM_INSERT);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void itemMoveBetweenAdaptersLeavesSlotUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.itemMoveBetweenAdaptersLeavesSlotUnchanged(helper, ITEM_MOVE);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void fluidMoveBetweenAdaptersLeavesTankUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.fluidMoveBetweenAdaptersLeavesTankUnchanged(helper, FLUID_MOVE);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void simulatedExtractLeavesDeviceSlotUntouched(final GameTestHelper helper) {
        CapabilityAdapterTests.extractLeavesDeviceSlotUntouched(helper, SIMULATED_EXTRACT);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void abortedExtractLeavesDeviceSlotUntouched(final GameTestHelper helper) {
        CapabilityAdapterTests.extractLeavesDeviceSlotUntouched(helper, ABORTED_ITEM_EXTRACT);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void extractFromLargeSlotYieldsAtMostAStack(final GameTestHelper helper) {
        CapabilityAdapterTests.extractFromLargeSlotYieldsAtMostAStack(helper, LARGE_SLOT);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToInventory(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void slotCapabilityWritesToForeignInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToForeignInventory(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void slotCapabilityRespectsInsertRestrictions(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityRespectsInsertRestrictions(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void stackCapabilityWritesToStack(final GameTestHelper helper) {
        CapabilityAdapterTests.stackCapabilityWritesToStack(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void stackCapabilityKeepsItem(final GameTestHelper helper) {
        CapabilityAdapterTests.stackCapabilityKeepsItem(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerItemSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemSimulatedInsertDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerItemInsertAndExtract(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertAndExtract(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertReturnsRemainder(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerFluidSimulatedFillDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidSimulatedFillDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerFluidFillReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillReplacesItem(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerFluidFillSplitsStack(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillSplitsStack(helper);
    }

    @GameTest(template = TEMPLATE_ID)
    @TestHolder
    public static void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidDrainReplacesItem(helper);
    }

    // --------------------------------------------------------------------- //

    private static EnergyHandler platformEnergy(final GameTestHelper helper) {
        final EnergyHandler handler = helper.getLevel().getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(DEVICE_POS), null);
        if (handler == null) {
            throw new GameTestAssertException(Component.literal("charger is not visible to the energy capability"), 0);
        }
        return handler;
    }

    private static ResourceHandler<ItemResource> platformItems(final GameTestHelper helper) {
        final ResourceHandler<ItemResource> handler = helper.getLevel().getCapability(Capabilities.Item.BLOCK, helper.absolutePos(DEVICE_POS), null);
        if (handler == null) {
            throw new GameTestAssertException(Component.literal("computer is not visible to the item capability"), 0);
        }
        return handler;
    }

    // --------------------------------------------------------------------- //

    private CapabilityAdapterTestsNeoForge() {
    }
}

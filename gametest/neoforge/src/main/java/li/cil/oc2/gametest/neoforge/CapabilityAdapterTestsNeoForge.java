/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.CapabilityAdapterTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;

import static li.cil.oc2.gametest.CapabilityAdapterTests.ENERGY;
import static li.cil.oc2.gametest.CapabilityAdapterTests.ITEMS;
import static li.cil.oc2.gametest.CapabilityAdapterTests.SIMULATED_EXTRACT;
import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.util.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.util.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class CapabilityAdapterTestsNeoForge {
    @GameTest(template = TEMPLATE)
    public static void simulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void committedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void simulatedExtractDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedExtractDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void committedExtractMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedExtractMutatesByReportedAmount(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void insertClampsToCapacity(final GameTestHelper helper) {
        CapabilityAdapterTests.insertClampsToCapacity(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void extractClampsToContents(final GameTestHelper helper) {
        CapabilityAdapterTests.extractClampsToContents(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void simulatedItemInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedItemInsertDoesNotMutate(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public static void committedItemInsertMutates(final GameTestHelper helper) {
        CapabilityAdapterTests.committedItemInsertMutates(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public static void itemRoundTripPreservesIdentity(final GameTestHelper helper) {
        CapabilityAdapterTests.itemRoundTripPreservesIdentity(helper, ITEMS);
    }

    @GameTest(template = TEMPLATE)
    public static void simulatedExtractLeavesDeviceSlotUntouched(final GameTestHelper helper) {
        CapabilityAdapterTests.extractLeavesDeviceSlotUntouched(helper, SIMULATED_EXTRACT);
    }

    @GameTest(template = TEMPLATE)
    public static void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        CapabilityAdapterTests.slotCapabilityWritesToInventory(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerItemSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemSimulatedInsertDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerItemInsertAndExtract(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertAndExtract(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        CapabilityAdapterTests.containerItemInsertReturnsRemainder(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidSimulatedFillDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidSimulatedFillDoesNotMutate(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidFillReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillReplacesItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidFillSplitsStack(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidFillSplitsStack(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        CapabilityAdapterTests.containerFluidDrainReplacesItem(helper);
    }

    @GameTest(template = TEMPLATE)
    public static void occupiedSlotStillAcceptsMatchingItems(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, Blocks.COMPUTER.get());
        final IItemHandler handler = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(DEVICE_POS), null);
        if (handler == null) {
            throw new GameTestAssertException("computer has no item handler");
        }

        final ItemStack memory = new ItemStack(Items.MEMORY_SMALL.get());
        int slot = 0;
        while (!handler.insertItem(slot, memory, true).isEmpty()) {
            slot++;
        }
        handler.insertItem(slot, memory.copy(), false);

        if (!handler.isItemValid(slot, memory)) {
            throw new GameTestAssertException("an occupied memory slot should still accept memory");
        }
        if (handler.isItemValid(slot, new ItemStack(net.minecraft.world.item.Items.DIRT))) {
            throw new GameTestAssertException("a memory slot should not accept dirt");
        }
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private CapabilityAdapterTestsNeoForge() {
    }
}

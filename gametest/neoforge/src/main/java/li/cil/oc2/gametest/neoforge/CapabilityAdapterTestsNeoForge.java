/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.EnergyStorage;
import li.cil.oc2.gametest.CapabilityAdapterTests;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.function.Function;

import static li.cil.oc2.gametest.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.TestSupport.MOD_ID;
import static li.cil.oc2.gametest.TestSupport.TEMPLATE;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class CapabilityAdapterTestsNeoForge {
    private static final Function<GameTestHelper, EnergyStorage> ENERGY = helper -> {
        helper.setBlock(DEVICE_POS, Blocks.CHARGER.get());
        return Capabilities.get(blockEntity(helper), Capabilities.ENERGY_STORAGE, null);
    };
    private static final Function<GameTestHelper, ItemHandler> ITEMS = helper -> {
        helper.setBlock(DEVICE_POS, Blocks.COMPUTER.get());
        return Capabilities.get(blockEntity(helper), Capabilities.ITEM_HANDLER, null);
    };
    private static final CapabilityAdapterTests.EnergyOperation ABORTED_INSERT =
            (helper, storage, amount) -> storage.receiveEnergy(amount, true);
    private static final CapabilityAdapterTests.EnergyOperation ABORTED_EXTRACT =
            (helper, storage, amount) -> storage.extractEnergy(amount, true);
    private static final CapabilityAdapterTests.ItemOperation ABORTED_ITEM_INSERT =
            (helper, handler, slot, stack) -> handler.insertItem(slot, stack, true);

    // ------------------------------------------------------------- //

    @GameTest(template = TEMPLATE)
    public static void simulatedInsertDoesNotMutate(final GameTestHelper helper) {
        CapabilityAdapterTests.simulatedInsertDoesNotMutate(helper, ENERGY);
    }

    @GameTest(template = TEMPLATE)
    public static void committedInsertMutatesByReportedAmount(final GameTestHelper helper) {
        CapabilityAdapterTests.committedInsertMutatesByReportedAmount(helper, ENERGY);
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
    public static void abortedInsertLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedInsertLeavesStorageUnchanged(helper, ENERGY, ABORTED_INSERT);
    }

    @GameTest(template = TEMPLATE)
    public static void abortedExtractLeavesStorageUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedExtractLeavesStorageUnchanged(helper, ENERGY, ABORTED_EXTRACT);
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
    public static void abortedItemInsertLeavesHandlerUnchanged(final GameTestHelper helper) {
        CapabilityAdapterTests.abortedItemInsertLeavesHandlerUnchanged(helper, ITEMS, ABORTED_ITEM_INSERT);
    }

    // ------------------------------------------------------------- //

    private static BlockEntity blockEntity(final GameTestHelper helper) {
        final BlockEntity blockEntity = helper.getBlockEntity(DEVICE_POS);
        if (blockEntity == null) {
            throw new GameTestAssertException("no block entity at " + DEVICE_POS);
        }
        return blockEntity;
    }

    private CapabilityAdapterTestsNeoForge() {
    }
}

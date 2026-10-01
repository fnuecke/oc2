/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ContainerFluidHandler;
import li.cil.oc2.common.container.ContainerItemHandler;
import li.cil.oc2.common.container.ItemStackHandler;
import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.item.Items;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

import java.util.function.Function;

public final class CapabilityAdapterTests {
    @FunctionalInterface
    public interface EnergyOperation {
        long apply(GameTestHelper helper, EnergyHandler storage, long amount);
    }

    @FunctionalInterface
    public interface ItemOperation {
        ItemStack apply(GameTestHelper helper, ItemHandler handler, int slot, ItemStack stack);
    }

    private static final long AMOUNT = 100;
    private static final Item SHULKER_BOX = net.minecraft.world.item.Items.SHULKER_BOX;
    private static final Item DIRT = net.minecraft.world.item.Items.DIRT;
    private static final Item BUCKET = net.minecraft.world.item.Items.BUCKET;
    private static final Item WATER_BUCKET = net.minecraft.world.item.Items.WATER_BUCKET;

    // --------------------------------------------------------------------- //

    public static void simulatedInsertDoesNotMutate(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        final long before = storage.getEnergyStored();

        final long accepted = storage.receiveEnergy(AMOUNT, true);

        assertEquals("simulated insert should report the accepted amount", AMOUNT, accepted);
        assertEquals("simulated insert must not change stored energy", before, storage.getEnergyStored());
        helper.succeed();
    }

    public static void committedInsertMutatesByReportedAmount(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        final long before = storage.getEnergyStored();

        final long accepted = storage.receiveEnergy(AMOUNT, false);

        assertEquals("committed insert should report the accepted amount", AMOUNT, accepted);
        assertEquals("committed insert must move exactly the reported amount",
            before + accepted, storage.getEnergyStored());
        helper.succeed();
    }

    public static void simulatedExtractDoesNotMutate(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        storage.receiveEnergy(AMOUNT, false);
        final long before = storage.getEnergyStored();

        final long extracted = storage.extractEnergy(AMOUNT, true);

        assertEquals("simulated extract should report the extracted amount", AMOUNT, extracted);
        assertEquals("simulated extract must not change stored energy", before, storage.getEnergyStored());
        helper.succeed();
    }

    public static void committedExtractMutatesByReportedAmount(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        storage.receiveEnergy(AMOUNT, false);
        final long before = storage.getEnergyStored();

        final long extracted = storage.extractEnergy(AMOUNT, false);

        assertEquals("committed extract should report the extracted amount", AMOUNT, extracted);
        assertEquals("committed extract must move exactly the reported amount",
            before - extracted, storage.getEnergyStored());
        helper.succeed();
    }

    public static void insertClampsToCapacity(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        final long capacity = storage.getMaxEnergyStored();
        if (capacity <= 0) {
            throw new GameTestAssertException("expected a positive capacity, was " + capacity);
        }

        final long accepted = storage.receiveEnergy(capacity * 2, false);

        assertEquals("insert must clamp to the remaining capacity", capacity, accepted);
        assertEquals("storage must end up exactly full", capacity, storage.getEnergyStored());
        helper.succeed();
    }

    public static void extractClampsToContents(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        storage.receiveEnergy(AMOUNT, false);

        final long extracted = storage.extractEnergy(AMOUNT * 10, false);

        assertEquals("extract must clamp to what is stored", AMOUNT, extracted);
        assertEquals("storage must end up empty", 0, storage.getEnergyStored());
        helper.succeed();
    }

    public static void abortedInsertLeavesStorageUnchanged(final GameTestHelper helper,
                                                           final Function<GameTestHelper, EnergyHandler> energy,
                                                           final EnergyOperation abortedInsert) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        final long before = storage.getEnergyStored();

        final long accepted = abortedInsert.apply(helper, storage, AMOUNT);

        assertEquals("an aborted insert should still report what it would have accepted", AMOUNT, accepted);
        assertEquals("an aborted insert must leave stored energy untouched", before, storage.getEnergyStored());
        helper.succeed();
    }

    public static void abortedExtractLeavesStorageUnchanged(final GameTestHelper helper,
                                                            final Function<GameTestHelper, EnergyHandler> energy,
                                                            final EnergyOperation abortedExtract) {
        final EnergyHandler storage = require(energy.apply(helper), "energy storage");
        storage.receiveEnergy(AMOUNT, false);
        final long before = storage.getEnergyStored();

        final long extracted = abortedExtract.apply(helper, storage, AMOUNT);

        assertEquals("an aborted extract should still report what it would have moved", AMOUNT, extracted);
        assertEquals("an aborted extract must leave stored energy untouched", before, storage.getEnergyStored());
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    public static void simulatedItemInsertDoesNotMutate(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> items) {
        final ItemHandler handler = require(items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);
        final ItemStack before = handler.getStackInSlot(slot).copy();

        final ItemStack remainder = handler.insertItem(slot, stack.copy(), true);

        assertEquals("simulated insert should consume the whole stack", 0, remainder.getCount());
        if (!ItemStack.matches(before, handler.getStackInSlot(slot))) {
            throw new GameTestAssertException("simulated insert must not change slot " + slot);
        }
        helper.succeed();
    }

    public static void committedItemInsertMutates(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> items) {
        final ItemHandler handler = require(items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);

        final ItemStack remainder = handler.insertItem(slot, stack.copy(), false);

        assertEquals("committed insert should consume the whole stack", 0, remainder.getCount());
        final ItemStack inSlot = handler.getStackInSlot(slot);
        if (!inSlot.is(stack.getItem())) {
            throw new GameTestAssertException("committed insert did not land in slot " + slot + ", found " + inSlot);
        }
        assertEquals("committed insert should have moved one item", 1, inSlot.getCount());
        helper.succeed();
    }

    public static void itemRoundTripPreservesIdentity(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> items) {
        final ItemHandler handler = require(items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);
        handler.insertItem(slot, stack.copy(), false);

        final ItemStack extracted = handler.extractItem(slot, 1, false);

        if (!extracted.is(stack.getItem())) {
            throw new GameTestAssertException("round trip changed the item: expected "
                + stack.getItem() + ", got " + extracted);
        }
        assertEquals("round trip changed the count", 1, extracted.getCount());
        assertEquals("slot should be empty again", 0, handler.getStackInSlot(slot).getCount());
        helper.succeed();
    }

    public static void abortedItemInsertLeavesHandlerUnchanged(final GameTestHelper helper,
                                                               final Function<GameTestHelper, ItemHandler> items,
                                                               final ItemOperation abortedInsert) {
        final ItemHandler handler = require(items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);
        final ItemStack before = handler.getStackInSlot(slot).copy();

        final ItemStack remainder = abortedInsert.apply(helper, handler, slot, stack.copy());

        assertEquals("aborted insert should report what it would move", 0, remainder.getCount());
        if (!ItemStack.matches(before, handler.getStackInSlot(slot))) {
            throw new GameTestAssertException("an aborted insert must leave slot " + slot
                + " untouched, found " + handler.getStackInSlot(slot));
        }
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    public static void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = require(Capabilities.get(inventory, 0, Capabilities.ITEM_HANDLER), "shulker box item handler");

        final ItemStack remainder = handler.insertItem(0, new ItemStack(DIRT, 5), false);

        assertEquals("insert should consume the whole stack", 0, remainder.getCount());
        final ItemHandler reread = require(Capabilities.get(inventory, 0, Capabilities.ITEM_HANDLER), "shulker box item handler");
        assertStack("shulker box contents", reread.getStackInSlot(0), DIRT, 5);
        helper.succeed();
    }

    public static void containerItemSimulatedInsertDoesNotMutate(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = require(ContainerItemHandler.of(inventory, 0), "container item handler");
        final ItemStack before = inventory.getStackInSlot(0).copy();

        final ItemStack remainder = handler.insertItem(0, new ItemStack(DIRT, 5), true);

        assertEquals("simulated insert should consume the whole stack", 0, remainder.getCount());
        if (!ItemStack.matches(before, inventory.getStackInSlot(0))) {
            throw new GameTestAssertException("simulated insert must not change the container item, found " + inventory.getStackInSlot(0));
        }
        helper.succeed();
    }

    public static void containerItemInsertAndExtract(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = require(ContainerItemHandler.of(inventory, 0), "container item handler");

        final ItemStack remainder = handler.insertItem(0, new ItemStack(DIRT, 5), false);
        assertEquals("insert should consume the whole stack", 0, remainder.getCount());
        assertStack("contents after insert", handler.getStackInSlot(0), DIRT, 5);

        final ItemStack extracted = handler.extractItem(0, 3, false);
        assertStack("extracted stack", extracted, DIRT, 3);
        assertStack("contents after extract", handler.getStackInSlot(0), DIRT, 2);
        assertStack("container item", inventory.getStackInSlot(0), SHULKER_BOX, 1);
        helper.succeed();
    }

    public static void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = require(ContainerItemHandler.of(inventory, 0), "container item handler");

        final ItemStack remainder = handler.insertItem(0, new ItemStack(DIRT, 70), false);

        assertStack("remainder", remainder, DIRT, 6);
        assertStack("contents after insert", handler.getStackInSlot(0), DIRT, 64);
        helper.succeed();
    }

    public static void containerFluidSimulatedFillDoesNotMutate(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(BUCKET));
        final FluidHandler handler = require(ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final int filled = handler.fill(water(), true);

        assertEquals("simulated fill should report a full bucket", FluidHandler.BUCKET, filled);
        assertStack("container item", inventory.getStackInSlot(0), BUCKET, 1);
        helper.succeed();
    }

    public static void containerFluidFillReplacesItem(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(BUCKET));
        final FluidHandler handler = require(ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final int filled = handler.fill(water(), false);

        assertEquals("fill should accept a full bucket", FluidHandler.BUCKET, filled);
        assertStack("container item", inventory.getStackInSlot(0), WATER_BUCKET, 1);
        helper.succeed();
    }

    public static void containerFluidFillSplitsStack(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(BUCKET, 3));
        final FluidHandler handler = require(ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final int filled = handler.fill(water(), false);

        assertEquals("fill should accept a full bucket", FluidHandler.BUCKET, filled);
        assertStack("remaining empty buckets", inventory.getStackInSlot(0), BUCKET, 2);
        assertStack("filled bucket", inventory.getStackInSlot(1), WATER_BUCKET, 1);
        helper.succeed();
    }

    public static void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(WATER_BUCKET));
        final FluidHandler handler = require(ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final FluidStack drained = handler.drain(FluidHandler.BUCKET, false);

        assertEquals("drain should yield a full bucket", FluidHandler.BUCKET, drained.amount());
        if (!drained.isSameFluid(water())) {
            throw new GameTestAssertException("drained the wrong fluid: " + drained);
        }
        assertStack("container item", inventory.getStackInSlot(0), BUCKET, 1);
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static ItemStackHandler inventory(final ItemStack stack) {
        final ItemStackHandler inventory = new ItemStackHandler(2);
        inventory.setStackInSlot(0, stack);
        return inventory;
    }

    private static FluidStack water() {
        return new FluidStack(Fluids.WATER, FluidHandler.BUCKET);
    }

    private static void assertStack(final String what, final ItemStack stack, final Item item, final int count) {
        if (!stack.is(item) || stack.getCount() != count) {
            throw new GameTestAssertException(what + ": expected " + count + " " + item + ", got " + stack);
        }
    }

    private static int acceptingSlot(final ItemHandler handler, final ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.insertItem(slot, stack.copy(), true).getCount() < stack.getCount()) {
                return slot;
            }
        }
        throw new GameTestAssertException("no slot accepts " + stack + " in a handler with "
            + handler.getSlots() + " slot(s)");
    }

    private static <T> T require(final T value, final String what) {
        if (value == null) {
            throw new GameTestAssertException(what + " was not available through the capability");
        }
        return value;
    }

    private static void assertEquals(final String what, final long expected, final long actual) {
        if (expected != actual) {
            throw new GameTestAssertException(what + ": expected " + expected + ", got " + actual);
        }
    }

    // --------------------------------------------------------------------- //

    private CapabilityAdapterTests() {
    }
}

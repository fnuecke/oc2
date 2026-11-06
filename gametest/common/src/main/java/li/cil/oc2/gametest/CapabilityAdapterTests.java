/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ContainerFluidHandler;
import li.cil.oc2.common.container.ContainerItemHandler;
import li.cil.oc2.common.container.ItemStackHandler;
import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.fluid.FluidTank;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.vm.AbstractVMItemStackHandlers;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.FurnaceBlockEntity;
import net.minecraft.world.level.material.Fluids;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import java.util.function.Function;

import static li.cil.oc2.gametest.util.TestSupport.DEVICE_POS;
import static li.cil.oc2.gametest.util.TestSupport.abort;
import static li.cil.oc2.gametest.util.TestSupport.assertEquals;
import static li.cil.oc2.gametest.util.TestSupport.assertNotNull;

public final class CapabilityAdapterTests {
    @FunctionalInterface
    public interface EnergyOperation {
        long apply(GameTestHelper helper, EnergyHandler storage, long amount);
    }

    @FunctionalInterface
    public interface ItemOperation {
        ItemStack apply(GameTestHelper helper, ItemHandler handler, int slot, ItemStack stack);
    }

    @FunctionalInterface
    public interface ItemMove {
        int apply(GameTestHelper helper, int slot, ItemStack stack, boolean commit);
    }

    @FunctionalInterface
    public interface FluidMove {
        int apply(FluidHandler handler, FluidStack stack, boolean commit);
    }

    public static final Function<GameTestHelper, EnergyHandler> ENERGY = helper -> {
        helper.setBlock(DEVICE_POS, Blocks.CHARGER.get());
        return Capabilities.get(blockEntity(helper), Capabilities.ENERGY_STORAGE, null);
    };
    public static final Function<GameTestHelper, ItemHandler> ITEMS = helper -> {
        helper.setBlock(DEVICE_POS, Blocks.COMPUTER.get());
        return Capabilities.get(blockEntity(helper), Capabilities.ITEM_HANDLER, null);
    };
    public static final ItemOperation UNCOMMITTED_EXTRACT = (helper, handler, slot, stack) ->
        abort(transaction -> handler.extractItem(slot, stack.getCount(), transaction));
    public static final ItemOperation UNCOMMITTED_INSERT = (helper, handler, slot, stack) ->
        abort(transaction -> handler.insertItem(slot, stack, transaction));
    public static final Item LARGE_SLOT_ITEM = net.minecraft.world.item.Items.DIRT;
    public static final int LARGE_SLOT_AMOUNT = 200;
    public static final Function<GameTestHelper, ItemHandler> FOREIGN_ITEMS = helper -> {
        helper.setBlock(DEVICE_POS, net.minecraft.world.level.block.Blocks.CHEST);
        return Capabilities.get(blockEntity(helper), Capabilities.ITEM_HANDLER, null);
    };

    private static final long AMOUNT = 100;
    private static final Item SHULKER_BOX = net.minecraft.world.item.Items.SHULKER_BOX;
    private static final Item DIRT = net.minecraft.world.item.Items.DIRT;
    private static final Item BUCKET = net.minecraft.world.item.Items.BUCKET;
    private static final Item WATER_BUCKET = net.minecraft.world.item.Items.WATER_BUCKET;
    private static final int FURNACE_RESULT_SLOT = 2;

    // --------------------------------------------------------------------- //

    public static void uncommittedInsertDoesNotMutate(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        final long before = storage.getEnergyStored();

        final long accepted = abort(transaction -> storage.receiveEnergy(AMOUNT, transaction));

        assertEquals(helper, "uncommitted insert should report the accepted amount", AMOUNT, accepted);
        assertEquals(helper, "uncommitted insert must not change stored energy", before, storage.getEnergyStored());
        helper.succeed();
    }

    public static void committedInsertMutatesByReportedAmount(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        final long before = storage.getEnergyStored();

        final long accepted = Transaction.run(transaction -> storage.receiveEnergy(AMOUNT, transaction));

        assertEquals(helper, "committed insert should report the accepted amount", AMOUNT, accepted);
        assertEquals(helper, "committed insert must move exactly the reported amount",
            before + accepted, storage.getEnergyStored());
        helper.succeed();
    }

    public static void uncommittedExtractDoesNotMutate(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        Transaction.run(transaction -> storage.receiveEnergy(AMOUNT, transaction));
        final long before = storage.getEnergyStored();

        final long extracted = abort(transaction -> storage.extractEnergy(AMOUNT, transaction));

        assertEquals(helper, "uncommitted extract should report the extracted amount", AMOUNT, extracted);
        assertEquals(helper, "uncommitted extract must not change stored energy", before, storage.getEnergyStored());
        helper.succeed();
    }

    public static void committedExtractMutatesByReportedAmount(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        Transaction.run(transaction -> storage.receiveEnergy(AMOUNT, transaction));
        final long before = storage.getEnergyStored();

        final long extracted = Transaction.run(transaction -> storage.extractEnergy(AMOUNT, transaction));

        assertEquals(helper, "committed extract should report the extracted amount", AMOUNT, extracted);
        assertEquals(helper, "committed extract must move exactly the reported amount",
            before - extracted, storage.getEnergyStored());
        helper.succeed();
    }

    public static void insertClampsToCapacity(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        final long capacity = storage.getMaxEnergyStored();
        if (capacity <= 0) {
            throw new GameTestAssertException(Component.literal("expected a positive capacity, was " + capacity), 0);
        }

        final long accepted = Transaction.run(transaction -> storage.receiveEnergy(capacity * 2, transaction));

        assertEquals(helper, "insert must clamp to the remaining capacity", capacity, accepted);
        assertEquals(helper, "storage must end up exactly full", capacity, storage.getEnergyStored());
        helper.succeed();
    }

    public static void extractClampsToContents(final GameTestHelper helper, final Function<GameTestHelper, EnergyHandler> energy) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        Transaction.run(transaction -> storage.receiveEnergy(AMOUNT, transaction));

        final long extracted = Transaction.run(transaction -> storage.extractEnergy(AMOUNT * 10, transaction));

        assertEquals(helper, "extract must clamp to what is stored", AMOUNT, extracted);
        assertEquals(helper, "storage must end up empty", 0, storage.getEnergyStored());
        helper.succeed();
    }

    public static void abortedInsertLeavesStorageUnchanged(final GameTestHelper helper,
                                                           final Function<GameTestHelper, EnergyHandler> energy,
                                                           final EnergyOperation abortedInsert) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        final long before = storage.getEnergyStored();

        final long accepted = abortedInsert.apply(helper, storage, AMOUNT);

        assertEquals(helper, "an aborted insert should still report what it would have accepted", AMOUNT, accepted);
        assertEquals(helper, "an aborted insert must leave stored energy untouched", before, storage.getEnergyStored());
        helper.succeed();
    }

    public static void abortedExtractLeavesStorageUnchanged(final GameTestHelper helper,
                                                            final Function<GameTestHelper, EnergyHandler> energy,
                                                            final EnergyOperation abortedExtract) {
        final EnergyHandler storage = assertNotNull(helper, energy.apply(helper), "energy storage");
        Transaction.run(transaction -> storage.receiveEnergy(AMOUNT, transaction));
        final long before = storage.getEnergyStored();

        final long extracted = abortedExtract.apply(helper, storage, AMOUNT);

        assertEquals(helper, "an aborted extract should still report what it would have moved", AMOUNT, extracted);
        assertEquals(helper, "an aborted extract must leave stored energy untouched", before, storage.getEnergyStored());
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    public static void uncommittedItemInsertDoesNotMutate(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> items) {
        final ItemHandler handler = assertNotNull(helper, items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);
        final ItemStack before = handler.getStackInSlot(slot).copy();

        final ItemStack remainder = abort(transaction -> handler.insertItem(slot, stack.copy(), transaction));

        assertEquals(helper, "uncommitted insert should consume the whole stack", 0, remainder.getCount());
        if (!ItemStack.matches(before, handler.getStackInSlot(slot))) {
            throw new GameTestAssertException(Component.literal("uncommitted insert must not change slot " + slot), 0);
        }
        helper.succeed();
    }

    public static void committedItemInsertMutates(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> items) {
        final ItemHandler handler = assertNotNull(helper, items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);

        final ItemStack remainder = Transaction.run(transaction -> handler.insertItem(slot, stack.copy(), transaction));

        assertEquals(helper, "committed insert should consume the whole stack", 0, remainder.getCount());
        final ItemStack inSlot = handler.getStackInSlot(slot);
        if (!inSlot.is(stack.getItem())) {
            throw new GameTestAssertException(Component.literal("committed insert did not land in slot " + slot + ", found " + inSlot), 0);
        }
        assertEquals(helper, "committed insert should have moved one item", 1, inSlot.getCount());
        helper.succeed();
    }

    public static void itemRoundTripPreservesIdentity(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> items) {
        final ItemHandler handler = assertNotNull(helper, items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);
        Transaction.run(transaction -> handler.insertItem(slot, stack.copy(), transaction));

        final ItemStack extracted = Transaction.run(transaction -> handler.extractItem(slot, 1, transaction));

        if (!extracted.is(stack.getItem())) {
            throw new GameTestAssertException(Component.literal("round trip changed the item: expected "
                + stack.getItem() + ", got " + extracted), 0);
        }
        assertEquals(helper, "round trip changed the count", 1, extracted.getCount());
        assertEquals(helper, "slot should be empty again", 0, handler.getStackInSlot(slot).getCount());
        helper.succeed();
    }

    public static void abortedItemInsertLeavesHandlerUnchanged(final GameTestHelper helper,
                                                               final Function<GameTestHelper, ItemHandler> items,
                                                               final ItemOperation abortedInsert) {
        final ItemHandler handler = assertNotNull(helper, items.apply(helper), "item handler");
        final ItemStack stack = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, stack);
        final ItemStack before = handler.getStackInSlot(slot).copy();

        final ItemStack remainder = abortedInsert.apply(helper, handler, slot, stack.copy());

        assertEquals(helper, "aborted insert should report what it would move", 0, remainder.getCount());
        if (!ItemStack.matches(before, handler.getStackInSlot(slot))) {
            throw new GameTestAssertException(Component.literal("an aborted insert must leave slot " + slot
                + " untouched, found " + handler.getStackInSlot(slot)), 0);
        }
        helper.succeed();
    }

    public static void extractLeavesDeviceSlotUntouched(final GameTestHelper helper, final ItemOperation extract) {
        final ItemHandler handler = assertNotNull(helper, ITEMS.apply(helper), "item handler");
        final AbstractVMItemStackHandlers computer = (AbstractVMItemStackHandlers)
            helper.getBlockEntity(DEVICE_POS, ComputerBlockEntity.class).getItemStackHandlers();
        final ItemStack memory = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, memory);
        Transaction.run(transaction -> handler.insertItem(slot, memory, transaction));
        final ItemStack inSlot = computer.combinedItemHandlers.getStackInSlot(slot);
        final Set<Device> devices = Collections.newSetFromMap(new IdentityHashMap<>());
        devices.addAll(computer.getDevices());

        final ItemStack extracted = extract.apply(helper, handler, slot, inSlot.copy());

        assertStack("reported extraction", extracted, memory.getItem(), 1);
        if (computer.combinedItemHandlers.getStackInSlot(slot) != inSlot) {
            throw new GameTestAssertException(Component.literal("the device slot holds a different stack: "
                + computer.combinedItemHandlers.getStackInSlot(slot)), 0);
        }
        if (computer.getDevices().size() != devices.size() || !devices.containsAll(computer.getDevices())) {
            throw new GameTestAssertException(Component.literal("the devices were rebuilt: " + computer.getDevices()), 0);
        }
        helper.succeed();
    }

    public static void abortedInsertLeavesDevicesUntouched(final GameTestHelper helper, final ItemOperation abortedInsert) {
        final ItemHandler handler = assertNotNull(helper, ITEMS.apply(helper), "item handler");
        final AbstractVMItemStackHandlers computer = (AbstractVMItemStackHandlers)
            helper.getBlockEntity(DEVICE_POS, ComputerBlockEntity.class).getItemStackHandlers();
        final ItemStack memory = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, memory);
        final Set<Device> devices = Collections.newSetFromMap(new IdentityHashMap<>());
        devices.addAll(computer.getDevices());

        final ItemStack remainder = abortedInsert.apply(helper, handler, slot, memory.copy());

        assertEquals(helper, "aborted insert should report what it would move", 0, remainder.getCount());
        if (!computer.combinedItemHandlers.getStackInSlot(slot).isEmpty()) {
            throw new GameTestAssertException(Component.literal("the device slot holds a stack after an aborted insert: "
                + computer.combinedItemHandlers.getStackInSlot(slot)), 0);
        }
        if (computer.getDevices().size() != devices.size() || !devices.containsAll(computer.getDevices())) {
            throw new GameTestAssertException(Component.literal("the devices were rebuilt: " + computer.getDevices()), 0);
        }
        helper.succeed();
    }

    public static void itemMoveBetweenAdaptersLeavesSlotUnchanged(final GameTestHelper helper, final ItemMove move) {
        final ItemHandler handler = assertNotNull(helper, ITEMS.apply(helper), "item handler");
        final ItemStack memory = new ItemStack(Items.MEMORY_SMALL.get());
        final int slot = acceptingSlot(handler, memory);
        Transaction.run(transaction -> handler.insertItem(slot, memory.copy(), transaction));

        assertEquals(helper, "aborted move should report the inserted amount", 1, move.apply(helper, slot, memory, false));
        assertStack("slot after aborted move", handler.getStackInSlot(slot), memory.getItem(), 1);
        assertEquals(helper, "committed move should report the inserted amount", 1, move.apply(helper, slot, memory, true));
        assertStack("slot after committed move", handler.getStackInSlot(slot), memory.getItem(), 1);
        helper.succeed();
    }

    public static void fluidMoveBetweenAdaptersLeavesTankUnchanged(final GameTestHelper helper, final FluidMove move) {
        final FluidTank tank = new FluidTank(FluidHandler.BUCKET);
        Transaction.run(transaction -> tank.fill(water(), transaction));
        final FluidStack stack = water().withAmount(FluidHandler.BUCKET / 10);

        assertEquals(helper, "aborted move should report the inserted amount", stack.amount(), move.apply(tank, stack, false));
        assertEquals(helper, "tank after aborted move", FluidHandler.BUCKET, tank.getFluidInTank(0).amount());
        assertEquals(helper, "committed move should report the inserted amount", stack.amount(), move.apply(tank, stack, true));
        assertEquals(helper, "tank after committed move", FluidHandler.BUCKET, tank.getFluidInTank(0).amount());
        helper.succeed();
    }

    public static void extractFromLargeSlotYieldsAtMostAStack(final GameTestHelper helper, final Function<GameTestHelper, ItemHandler> largeSlot) {
        final ItemHandler handler = assertNotNull(helper, largeSlot.apply(helper), "item handler");
        final int stackSize = LARGE_SLOT_ITEM.getDefaultMaxStackSize();

        assertStack("uncommitted extract", abort(transaction -> handler.extractItem(0, LARGE_SLOT_AMOUNT, transaction)), LARGE_SLOT_ITEM, stackSize);
        assertStack("extracted stack", Transaction.run(transaction -> handler.extractItem(0, LARGE_SLOT_AMOUNT, transaction)), LARGE_SLOT_ITEM, stackSize);
        assertStack("slot after extract", handler.getStackInSlot(0), LARGE_SLOT_ITEM, LARGE_SLOT_AMOUNT - stackSize);
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    public static void slotCapabilityWritesToInventory(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = assertNotNull(helper, Capabilities.get(inventory, 0, Capabilities.ITEM_HANDLER), "shulker box item handler");

        final ItemStack remainder = Transaction.run(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction));

        assertEquals(helper, "insert should consume the whole stack", 0, remainder.getCount());
        final ItemHandler reread = assertNotNull(helper, Capabilities.get(inventory, 0, Capabilities.ITEM_HANDLER), "shulker box item handler");
        assertStack("shulker box contents", reread.getStackInSlot(0), DIRT, 5);
        helper.succeed();
    }

    public static void slotCapabilityWritesToForeignInventory(final GameTestHelper helper) {
        final ItemHandler chest = assertNotNull(helper, FOREIGN_ITEMS.apply(helper), "chest item handler");
        final ChestBlockEntity blockEntity = helper.getBlockEntity(DEVICE_POS, ChestBlockEntity.class);
        blockEntity.setItem(0, new ItemStack(SHULKER_BOX));
        final ItemStack before = blockEntity.getItem(0).copy();
        final ItemHandler handler = assertNotNull(helper, Capabilities.get(chest, 0, Capabilities.ITEM_HANDLER), "shulker box item handler");

        assertEquals(helper, "uncommitted insert should consume the whole stack", 0, abort(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction)).getCount());
        if (!ItemStack.matches(before, blockEntity.getItem(0))) {
            throw new GameTestAssertException(Component.literal("uncommitted insert must not change the chest, found " + blockEntity.getItem(0)), 0);
        }

        assertEquals(helper, "insert should consume the whole stack", 0, Transaction.run(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction)).getCount());
        final ItemHandler reread = assertNotNull(helper, Capabilities.get(chest, 0, Capabilities.ITEM_HANDLER), "shulker box item handler");
        assertStack("shulker box contents", reread.getStackInSlot(0), DIRT, 5);
        assertStack("chest slot", blockEntity.getItem(0), SHULKER_BOX, 1);
        helper.succeed();
    }

    public static void slotCapabilityRespectsInsertRestrictions(final GameTestHelper helper) {
        helper.setBlock(DEVICE_POS, net.minecraft.world.level.block.Blocks.FURNACE);
        final FurnaceBlockEntity furnace = helper.getBlockEntity(DEVICE_POS, FurnaceBlockEntity.class);
        furnace.setItem(FURNACE_RESULT_SLOT, new ItemStack(SHULKER_BOX));
        final ItemHandler sided = assertNotNull(helper, Capabilities.get(furnace, Capabilities.ITEM_HANDLER, Direction.DOWN), "furnace item handler");
        final int slot = slotHolding(sided, SHULKER_BOX);
        final ItemHandler handler = assertNotNull(helper, Capabilities.get(sided, slot, Capabilities.ITEM_HANDLER), "shulker box item handler");

        final ItemStack remainder = Transaction.run(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction));

        assertEquals(helper, "a container item in an output slot must not be changed", 5, remainder.getCount());
        final ItemStack result = furnace.getItem(FURNACE_RESULT_SLOT);
        assertStack("furnace output slot", result, SHULKER_BOX, 1);
        if (!ItemStack.matches(result, new ItemStack(SHULKER_BOX))) {
            throw new GameTestAssertException(Component.literal("the container item in the output slot changed: " + result), 0);
        }
        helper.succeed();
    }

    public static void stackCapabilityWritesToStack(final GameTestHelper helper) {
        final ItemStack box = new ItemStack(SHULKER_BOX);
        final ItemHandler handler = assertNotNull(helper, Capabilities.get(box, Capabilities.ITEM_HANDLER), "shulker box item handler");

        assertEquals(helper, "uncommitted insert should consume the whole stack", 0, abort(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction)).getCount());
        if (!ItemStack.matches(box, new ItemStack(SHULKER_BOX))) {
            throw new GameTestAssertException(Component.literal("uncommitted insert must not change the stack, found " + box), 0);
        }

        assertEquals(helper, "insert should consume the whole stack", 0, Transaction.run(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction)).getCount());
        assertStack("contents through the same handler", handler.getStackInSlot(0), DIRT, 5);
        final ItemHandler reread = assertNotNull(helper, Capabilities.get(box, Capabilities.ITEM_HANDLER), "shulker box item handler");
        assertStack("contents of the stack", reread.getStackInSlot(0), DIRT, 5);

        assertStack("extracted stack", Transaction.run(transaction -> handler.extractItem(0, 5, transaction)), DIRT, 5);
        if (!ItemStack.matches(box, new ItemStack(SHULKER_BOX))) {
            throw new GameTestAssertException(Component.literal("emptying the stack should restore its default components, found " + box), 0);
        }
        helper.succeed();
    }

    public static void stackCapabilityKeepsItem(final GameTestHelper helper) {
        final ItemStack bucket = new ItemStack(BUCKET);
        final FluidHandler handler = assertNotNull(helper, Capabilities.get(bucket, Capabilities.FLUID_HANDLER), "bucket fluid handler");

        assertEquals(helper, "a fill that replaces the item must be rejected", 0, Transaction.run(transaction -> handler.fill(water(), transaction)));
        assertStack("bucket", bucket, BUCKET, 1);
        helper.succeed();
    }

    public static void containerItemUncommittedInsertDoesNotMutate(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = assertNotNull(helper, ContainerItemHandler.of(inventory, 0), "container item handler");
        final ItemStack before = inventory.getStackInSlot(0).copy();

        final ItemStack remainder = abort(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction));

        assertEquals(helper, "uncommitted insert should consume the whole stack", 0, remainder.getCount());
        if (!ItemStack.matches(before, inventory.getStackInSlot(0))) {
            throw new GameTestAssertException(Component.literal("uncommitted insert must not change the container item, found " + inventory.getStackInSlot(0)), 0);
        }
        helper.succeed();
    }

    public static void containerItemInsertAndExtract(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = assertNotNull(helper, ContainerItemHandler.of(inventory, 0), "container item handler");

        final ItemStack remainder = Transaction.run(transaction -> handler.insertItem(0, new ItemStack(DIRT, 5), transaction));
        assertEquals(helper, "insert should consume the whole stack", 0, remainder.getCount());
        assertStack("contents after insert", handler.getStackInSlot(0), DIRT, 5);

        final ItemStack extracted = Transaction.run(transaction -> handler.extractItem(0, 3, transaction));
        assertStack("extracted stack", extracted, DIRT, 3);
        assertStack("contents after extract", handler.getStackInSlot(0), DIRT, 2);
        assertStack("container item", inventory.getStackInSlot(0), SHULKER_BOX, 1);
        helper.succeed();
    }

    public static void containerItemInsertReturnsRemainder(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(SHULKER_BOX));
        final ItemHandler handler = assertNotNull(helper, ContainerItemHandler.of(inventory, 0), "container item handler");

        final ItemStack remainder = Transaction.run(transaction -> handler.insertItem(0, new ItemStack(DIRT, 70), transaction));

        assertStack("remainder", remainder, DIRT, 6);
        assertStack("contents after insert", handler.getStackInSlot(0), DIRT, 64);
        helper.succeed();
    }

    public static void containerFluidUncommittedFillDoesNotMutate(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(BUCKET));
        final FluidHandler handler = assertNotNull(helper, ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final int filled = abort(transaction -> handler.fill(water(), transaction));

        assertEquals(helper, "uncommitted fill should report a full bucket", FluidHandler.BUCKET, filled);
        assertStack("container item", inventory.getStackInSlot(0), BUCKET, 1);
        helper.succeed();
    }

    public static void containerFluidFillReplacesItem(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(BUCKET));
        final FluidHandler handler = assertNotNull(helper, ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final int filled = Transaction.run(transaction -> handler.fill(water(), transaction));

        assertEquals(helper, "fill should accept a full bucket", FluidHandler.BUCKET, filled);
        assertStack("container item", inventory.getStackInSlot(0), WATER_BUCKET, 1);
        helper.succeed();
    }

    public static void containerFluidFillSplitsStack(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(BUCKET, 3));
        final FluidHandler handler = assertNotNull(helper, ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final int filled = Transaction.run(transaction -> handler.fill(water(), transaction));

        assertEquals(helper, "fill should accept a full bucket", FluidHandler.BUCKET, filled);
        assertStack("remaining empty buckets", inventory.getStackInSlot(0), BUCKET, 2);
        assertStack("filled bucket", inventory.getStackInSlot(1), WATER_BUCKET, 1);
        helper.succeed();
    }

    public static void containerFluidDrainReplacesItem(final GameTestHelper helper) {
        final ItemStackHandler inventory = inventory(new ItemStack(WATER_BUCKET));
        final FluidHandler handler = assertNotNull(helper, ContainerFluidHandler.of(inventory, 0), "container fluid handler");

        final FluidStack drained = Transaction.run(transaction -> handler.drain(FluidHandler.BUCKET, transaction));

        assertEquals(helper, "drain should yield a full bucket", FluidHandler.BUCKET, drained.amount());
        if (!drained.isSameFluid(water())) {
            throw new GameTestAssertException(Component.literal("drained the wrong fluid: " + drained), 0);
        }
        assertStack("container item", inventory.getStackInSlot(0), BUCKET, 1);
        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static BlockEntity blockEntity(final GameTestHelper helper) {
        final BlockEntity blockEntity = helper.getBlockEntity(DEVICE_POS, BlockEntity.class);
        if (blockEntity == null) {
            throw new GameTestAssertException(Component.literal("no block entity at " + DEVICE_POS), 0);
        }
        return blockEntity;
    }

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
            throw new GameTestAssertException(Component.literal(what + ": expected " + count + " " + item + ", got " + stack), 0);
        }
    }

    private static int slotHolding(final ItemHandler handler, final Item item) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            if (handler.getStackInSlot(slot).is(item)) {
                return slot;
            }
        }
        throw new GameTestAssertException(Component.literal("no slot holds " + item), 0);
    }

    private static int acceptingSlot(final ItemHandler handler, final ItemStack stack) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            final int candidate = slot;
            if (abort(transaction -> handler.insertItem(candidate, stack.copy(), transaction)).getCount() < stack.getCount()) {
                return slot;
            }
        }
        throw new GameTestAssertException(Component.literal("no slot accepts " + stack + " in a handler with "
            + handler.getSlots() + " slot(s)"), 0);
    }

    // --------------------------------------------------------------------- //

    private CapabilityAdapterTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.bus.device.util.FluidHandlerDeviceUtils;
import li.cil.oc2.common.bus.device.util.FluidHandlerProtocol;
import li.cil.oc2.common.bus.device.util.ItemHandlerDeviceUtils;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.container.ItemHandlerUtils;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidHandlerUtils;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.util.FakePlayerUtils;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static java.util.Objects.requireNonNull;

@RPCDeviceDescription(typeNames = {"transposer"}, description = """
    Provided by the [transposer](../block/transposer.md) block.

    Allows inspecting and operating on block and entity containers. If neither are present, space permitting, items are dropped into the world or picked up, fluids are poured or drained, one bucket at a time.

    Sides without a container report no inventory or tank, which allows predicting world interaction, e.g. to ensure things are never dumped into the world.""")
@IODeviceDescription(name = "TRANSP", description = """
    Sides are numbered as in the "Sides" section above. Item and fluid ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup. Amounts are millibuckets, four bytes, low byte first.

    A side with neither a container nor available space, or a protected side, fails with `OCEARG`.""")
public final class TransposerBlockEntity extends ModBlockEntity {
    private static final String SIDE_OF_INVENTORY = "the side of the inventory to inspect.";
    private static final String SIDE_OF_CONTAINER = "the side of the container to inspect.";
    private static final String SLOT_TO_LOOK_AT = "the number of the slot to inspect.";
    private static final String TANK_TO_LOOK_AT = "the number of the tank to inspect.";
    private static final String SIDE_TO_TAKE_ITEMS_FROM = "the side of the inventory to take items from.";
    private static final String SLOT_TO_TAKE_ITEMS_FROM = "the number of the slot to take items from.";
    private static final String SIDE_TO_PUT_ITEMS_INTO = "the side of the inventory to put items into.";
    private static final String SLOT_TO_PUT_ITEMS_INTO = "the number of the slot to put items into.";
    private static final String ITEMS_TRANSFERRED = "the number of items transferred.";

    private static final int GET_ITEM_SLOT_COUNT_CODE = 1;
    private static final int GET_ITEM_SLOTS_CODE = 2;
    private static final int GET_ITEM_SLOT_LIMIT_CODE = 3;
    private static final int MOVE_ITEMS_CODE = 4;
    private static final int MOVE_ITEMS_ANY_CODE = 5;
    private static final int GET_FLUID_TANK_COUNT_CODE = 6;
    private static final int GET_FLUID_TANKS_CODE = 7;
    private static final int GET_FLUID_TANK_CAPACITY_CODE = 8;
    private static final int MOVE_FLUID_CODE = 9;

    // --------------------------------------------------------------------- //

    public TransposerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.TRANSPOSER.get(), pos, state);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many slots the inventory on the specified side has.",
        returnValueDescription = "the number of slots, or `0` if there is no inventory on that side.")
    public int getItemSlotCount(@Parameter(value = "side", description = SIDE_OF_INVENTORY) @Nullable final Side side) {
        final List<ItemHandler> handlers = getHandlers(side, Capabilities.ITEM_HANDLER);
        return handlers.isEmpty() ? 0 : handlers.getFirst().getSlots();
    }

    @Callback(description = "Gets what is in the specified slot of the inventory on the specified side.",
        returnValueDescription = "a table with the item information. Returns nothing for an empty slot.")
    public ItemStack getItemStackInSlot(@Parameter(value = "side", description = SIDE_OF_INVENTORY) @Nullable final Side side,
                                        @Parameter(value = "slot", description = SLOT_TO_LOOK_AT) final int slot) {
        final ItemHandler handler = requireItemHandlers(side).getFirst();
        return handler.getStackInSlot(ItemHandlerDeviceUtils.requireValidSlot(handler, slot));
    }

    @Callback(description = "Gets how many items the specified slot of the inventory on the specified side can hold.",
        returnValueDescription = "the most items the slot takes.")
    public int getItemSlotLimit(@Parameter(value = "side", description = SIDE_OF_INVENTORY) @Nullable final Side side,
                                @Parameter(value = "slot", description = SLOT_TO_LOOK_AT) final int slot) {
        final ItemHandler handler = requireItemHandlers(side).getFirst();
        return handler.getSlotLimit(ItemHandlerDeviceUtils.requireValidSlot(handler, slot));
    }

    @Callback(energy = 1, description = "Moves items from one inventory to another, from and into any slot. It moves as many items as the first source slot that yields anything gives and the first target slot that accepts anything takes, up to `count`.",
        returnValueDescription = ITEMS_TRANSFERRED)
    public int moveItems(@Parameter(value = "sourceSide", description = SIDE_TO_TAKE_ITEMS_FROM) @Nullable final Side sourceSide,
                         @Parameter(value = "targetSide", description = SIDE_TO_PUT_ITEMS_INTO) @Nullable final Side targetSide,
                         @Parameter(value = "count", description = "the most items to move.") final int count) {
        final boolean isSameSide = requireDirection(sourceSide) == requireDirection(targetSide);
        final List<ItemHandler> sources = requireItemHandlersOrWorld(sourceSide);
        final List<ItemHandler> targets = isSameSide ? sources : requireItemHandlersOrWorld(targetSide);
        return ItemHandlerUtils.transferFirst(sources, ItemHandlerUtils.ANY_SLOT, targets, ItemHandlerUtils.ANY_SLOT, 0, count, this::dropItemStack);
    }

    @Callback(energy = 1, description = "Moves items from one inventory slot to another. It moves as many items as the source slot yields and the target slot accepts, up to `count`. Only uses inventories, never open space.",
        returnValueDescription = ITEMS_TRANSFERRED)
    public int moveItems(@Parameter(value = "sourceSide", description = SIDE_TO_TAKE_ITEMS_FROM) @Nullable final Side sourceSide,
                         @Parameter(value = "sourceSlot", description = SLOT_TO_TAKE_ITEMS_FROM) final int sourceSlot,
                         @Parameter(value = "targetSide", description = SIDE_TO_PUT_ITEMS_INTO) @Nullable final Side targetSide,
                         @Parameter(value = "targetSlot", description = SLOT_TO_PUT_ITEMS_INTO) final int targetSlot,
                         @Parameter(value = "count", description = "the most items to move.") final int count) {
        final boolean isSameSide = requireDirection(sourceSide) == requireDirection(targetSide);
        final List<ItemHandler> sources = ItemHandlerDeviceUtils.requireItemHandlersWithSlot(requireItemHandlers(sourceSide), sourceSlot);
        final List<ItemHandler> targets = ItemHandlerDeviceUtils.requireItemHandlersWithSlot(isSameSide ? sources : requireItemHandlers(targetSide), targetSlot);
        return ItemHandlerUtils.transferFirst(sources, sourceSlot, targets, targetSlot, 0, count, this::dropItemStack);
    }

    @Callback(description = "Gets how many tanks the fluid container on the specified side has.",
        returnValueDescription = "the number of tanks, or `0` if there is no fluid container on that side.")
    public int getFluidTankCount(@Parameter(value = "side", description = SIDE_OF_CONTAINER) @Nullable final Side side) {
        final List<FluidHandler> handlers = getHandlers(side, Capabilities.FLUID_HANDLER);
        return handlers.isEmpty() ? 0 : handlers.getFirst().getTanks();
    }

    @Callback(description = "Gets what is in the specified tank of the container on the specified side.",
        returnValueDescription = "a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.")
    public FluidStack getFluidInTank(@Parameter(value = "side", description = SIDE_OF_CONTAINER) @Nullable final Side side,
                                     @Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank) {
        final FluidHandler handler = requireFluidHandlers(side).getFirst();
        return handler.getFluidInTank(FluidHandlerDeviceUtils.requireValidTank(handler, tank));
    }

    @Callback(description = "Gets how much the specified tank of the container on the specified side can hold.",
        returnValueDescription = "the capacity in millibuckets.")
    public int getFluidTankCapacity(@Parameter(value = "side", description = SIDE_OF_CONTAINER) @Nullable final Side side,
                                    @Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank) {
        final FluidHandler handler = requireFluidHandlers(side).getFirst();
        return handler.getTankCapacity(FluidHandlerDeviceUtils.requireValidTank(handler, tank));
    }

    @Callback(energy = 1, description = "Moves fluid from one container to another. It moves as much as the source yields and the target accepts, up to `amount`. A bucket is 1000.",
        returnValueDescription = "the amount transferred in millibuckets.")
    public int moveFluid(@Parameter(value = "sourceSide", description = "the side of the container to drain.") @Nullable final Side sourceSide,
                         @Parameter(value = "targetSide", description = "the side of the container to fill.") @Nullable final Side targetSide,
                         @Parameter(value = "amount", description = "maximum amount to move, in millibuckets.") final int amount) {
        final List<FluidHandler> sources = requireFluidHandlersOrWorld(sourceSide);
        final List<FluidHandler> targets = requireFluidHandlersOrWorld(targetSide);

        if (requireDirection(sourceSide) == requireDirection(targetSide)) {
            return 0;
        }

        return FluidHandlerUtils.transferFirst(sources, targets, amount);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ITEM_SLOT_COUNT_CODE,
        description = "Reads how many slots the inventory on that side has.",
        argumentsDescription = "one byte, the side.",
        resultsDescription = "one byte, the slot count, at most 255. A side without an inventory reads as 0.")
    public void getItemSlotCount(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getItemSlotCount(Side.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(value = GET_ITEM_SLOTS_CODE,
        description = "Reads a run of slots of the inventory on that side.",
        argumentsDescription = "three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.",
        resultsDescription = "four bytes per slot: the item as two bytes, the number of items up to 255, and damage.")
    public void getItemSlots(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlots(requireItemHandlers(Side.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(value = GET_ITEM_SLOT_LIMIT_CODE,
        description = "Reads how much a slot of the inventory on that side can hold.",
        argumentsDescription = "two bytes, the side and the slot.",
        resultsDescription = "one byte, the limit, at most 255.")
    public void getItemSlotLimit(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotLimit(requireItemHandlers(Side.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(value = MOVE_ITEMS_CODE, energy = 1,
        description = "Moves up to `count` items between two slots.",
        argumentsDescription = "five bytes, the side and slot to take from, the side and slot to put into, and how many items to move at most.",
        resultsDescription = "one byte, how many items were moved.")
    public void moveItems(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final int sourceSlot = arguments.readU8();
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int targetSlot = arguments.readU8();
        final int count = arguments.readU8();
        results.writeU8(moveItems(sourceSide, sourceSlot, targetSide, targetSlot, count));
    }

    @IOCallback(value = MOVE_ITEMS_ANY_CODE, energy = 1,
        description = "Moves up to `count` items from any slot into any slot.",
        argumentsDescription = "three bytes, the side to take from, the side to put into, and how many items to move at most.",
        resultsDescription = "one byte, how many items were moved.")
    public void moveItemsAny(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int count = arguments.readU8();
        results.writeU8(moveItems(sourceSide, targetSide, count));
    }

    @IOCallback(value = GET_FLUID_TANK_COUNT_CODE,
        description = "Reads how many tanks the container on that side has.",
        argumentsDescription = "one byte, the side.",
        resultsDescription = "one byte, the tank count, at most 255. A side without a fluid container reads as 0.")
    public void getFluidTankCount(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getFluidTankCount(Side.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(value = GET_FLUID_TANKS_CODE,
        description = "Reads a run of tanks of the container on that side.",
        argumentsDescription = "three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.",
        resultsDescription = "six bytes per tank: the fluid as two bytes and the amount as four bytes.")
    public void getFluidTanks(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(requireFluidHandlers(Side.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(value = GET_FLUID_TANK_CAPACITY_CODE,
        description = "Reads how much a tank of the container on that side can hold.",
        argumentsDescription = "two bytes, the side and the tank.",
        resultsDescription = "four bytes, the capacity.")
    public void getFluidTankCapacity(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(requireFluidHandlers(Side.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(value = MOVE_FLUID_CODE, energy = 1,
        description = "Moves up to `amount` millibuckets between two containers.",
        argumentsDescription = "six bytes, the side to drain, the side to fill, and four bytes for how much to move at most.",
        resultsDescription = "four bytes, how much was moved.")
    public void moveFluid(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int amount = (int) Math.min(arguments.readU32(), Integer.MAX_VALUE);
        results.writeU32(moveFluid(sourceSide, targetSide, amount));
    }

    // --------------------------------------------------------------------- //

    private Direction requireDirection(@Nullable final Side side) {
        if (side == null) {
            throw new IllegalArgumentException();
        }

        final Direction direction = HorizontalBlockUtils.toGlobal(getBlockState(), side);
        assert direction != null;
        return direction;
    }

    private ServerLevel serverLevel() {
        return (ServerLevel) requireNonNull(level);
    }

    private <T> List<T> getHandlers(@Nullable final Side side, final CapabilityType<T> type) {
        final Direction direction = requireDirection(side);
        return Capabilities.getAll(serverLevel(), getBlockPos().relative(direction), type, direction.getOpposite(), null);
    }

    private List<ItemHandler> requireItemHandlers(@Nullable final Side side) {
        final Direction direction = requireDirection(side);
        return ItemHandlerDeviceUtils.requireItemHandlers(serverLevel(), getBlockPos().relative(direction), direction.getOpposite(), null, side);
    }

    private List<ItemHandler> requireItemHandlersOrWorld(@Nullable final Side side) {
        final Direction direction = requireDirection(side);
        return ItemHandlerDeviceUtils.requireItemHandlersOrWorld(serverLevel(), getBlockPos().relative(direction), direction.getOpposite(),
            null, FakePlayerUtils.getFakePlayer(serverLevel()), side);
    }

    private List<FluidHandler> requireFluidHandlers(@Nullable final Side side) {
        final Direction direction = requireDirection(side);
        return FluidHandlerDeviceUtils.requireFluidHandlers(serverLevel(), getBlockPos().relative(direction), direction.getOpposite(), null, side);
    }

    private List<FluidHandler> requireFluidHandlersOrWorld(@Nullable final Side side) {
        final Direction direction = requireDirection(side);
        return FluidHandlerDeviceUtils.requireFluidHandlersOrWorld(serverLevel(), getBlockPos().relative(direction), direction.getOpposite(),
            null, FakePlayerUtils.getFakePlayer(serverLevel()), side);
    }

    private void dropItemStack(final ItemStack stack) {
        final BlockPos pos = getBlockPos();
        Containers.dropItemStack(requireNonNull(level), pos.getX(), pos.getY(), pos.getZ(), stack);
    }
}

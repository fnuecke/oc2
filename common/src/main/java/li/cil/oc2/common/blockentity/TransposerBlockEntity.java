/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.bus.device.util.FluidHandlerProtocol;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.util.FakePlayerUtils;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.io.IOException;

@RPCDeviceDescription(typeNames = {"transposer"}, description = """
    Provided by the [transposer](../block/transposer.md) block.

    ### Sides
    Sides name the container next to the transposer. Relative sides turn with the block: `front`, `back`, `left` and `right`, seen when looking at the primary face, the one with a single marking. Absolute sides always mean the same direction in the world: `north`, `south`, `west` and `east`. `up` and `down` mean the same thing either way.

    Sides may also be given as a number instead of a name. Numbers are relative: `0` is `down`, `1` is `up`, `2` is `back`, `3` is `front`, `4` is `left` and `5` is `right`.

    Sides that are protected, for example by spawn protection, fail with an error.""")
@IODeviceDescription(name = "TRANSP", description = """
    Sides are numbered as in the "Sides" section above. Item numbers are two bytes, low byte first, as on the `ITEMS` device. Fluids follow the same pattern; amounts are millibuckets, four bytes, low byte first, as on the `FLUIDS` device.

    A blocked target side, or a protected side, fails with `OCEARG`.""")
public final class TransposerBlockEntity extends ModBlockEntity {
    private static final String SIDE_OF_INVENTORY = "the side of the inventory to inspect.";
    private static final String SIDE_OF_CONTAINER = "the side of the container to inspect.";
    private static final String SLOT_TO_LOOK_AT = "the number of the slot to look at.";
    private static final String TANK_TO_LOOK_AT = "the number of the tank to look at.";
    private static final String SIDE_TO_TAKE_ITEMS_FROM = "the side of the inventory to take items from.";
    private static final String SLOT_TO_TAKE_ITEMS_FROM = "the number of the slot to take items from.";
    private static final String SIDE_TO_PUT_ITEMS_INTO = "the side of the inventory to put items into.";
    private static final String SLOT_TO_PUT_ITEMS_INTO = "the number of the slot to put items into.";
    private static final String SIDE_TO_DRAIN = "the side of the container to drain.";
    private static final String SIDE_TO_FILL = "the side of the container to fill.";
    private static final String ITEMS_TRANSFERRED = "the number of items transferred.";
    private static final String BUCKET_OR_NOTHING = "the amount transferred in millibuckets, `1000` or `0`.";

    private static final int GET_ITEM_SLOT_COUNT_CODE = 1;
    private static final int GET_ITEM_SLOTS_CODE = 2;
    private static final int GET_ITEM_SLOT_LIMIT_CODE = 3;
    private static final int GET_ITEM_NAME_CODE = 4;
    private static final int GET_ITEM_ID_CODE = 5;
    private static final int MOVE_ITEMS_CODE = 6;
    private static final int GET_FLUID_TANK_COUNT_CODE = 7;
    private static final int GET_FLUID_TANKS_CODE = 8;
    private static final int GET_FLUID_TANK_CAPACITY_CODE = 9;
    private static final int GET_FLUID_NAME_CODE = 10;
    private static final int GET_FLUID_ID_CODE = 11;
    private static final int MOVE_FLUID_CODE = 12;
    private static final int DROP_ITEMS_CODE = 13;
    private static final int TAKE_ITEMS_CODE = 14;
    private static final int FILL_FLUID_CODE = 15;
    private static final int DRAIN_FLUID_CODE = 16;

    // --------------------------------------------------------------------- //

    public TransposerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.TRANSPOSER.get(), pos, state);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many slots the inventory on the specified side has.",
        returnValueDescription = "the number of slots, or `0` if there is no inventory on that side.")
    public int getItemSlotCount(@Parameter(value = "side", description = SIDE_OF_INVENTORY) @Nullable final Side side) {
        final ItemHandler handler = getItemHandler(requireDirection(side));
        return handler != null ? handler.getSlots() : 0;
    }

    @Callback(description = "Gets what is in the specified slot of the inventory on the specified side.",
        returnValueDescription = "a table with the item information. Returns nothing for an empty slot.")
    public ItemStack getItemStackInSlot(@Parameter(value = "side", description = SIDE_OF_INVENTORY) @Nullable final Side side,
                                        @Parameter(value = "slot", description = SLOT_TO_LOOK_AT) final int slot) {
        final ItemHandler handler = requireItemHandler(side);
        return handler.getStackInSlot(ItemHandlerProtocol.requireValidSlot(handler, slot));
    }

    @Callback(description = "Gets how many items the specified slot of the inventory on the specified side can hold.",
        returnValueDescription = "the most items the slot takes.")
    public int getItemSlotLimit(@Parameter(value = "side", description = SIDE_OF_INVENTORY) @Nullable final Side side,
                                @Parameter(value = "slot", description = SLOT_TO_LOOK_AT) final int slot) {
        final ItemHandler handler = requireItemHandler(side);
        return handler.getSlotLimit(ItemHandlerProtocol.requireValidSlot(handler, slot));
    }

    @Callback(description = "Moves items from one inventory to another. It moves as many items as the source slot yields and the target slot accepts, up to `count`.",
        returnValueDescription = ITEMS_TRANSFERRED)
    public int moveItems(@Parameter(value = "sourceSide", description = SIDE_TO_TAKE_ITEMS_FROM) @Nullable final Side sourceSide,
                         @Parameter(value = "sourceSlot", description = SLOT_TO_TAKE_ITEMS_FROM) final int sourceSlot,
                         @Parameter(value = "targetSide", description = SIDE_TO_PUT_ITEMS_INTO) @Nullable final Side targetSide,
                         @Parameter(value = "targetSlot", description = SLOT_TO_PUT_ITEMS_INTO) final int targetSlot,
                         @Parameter(value = "count", description = "the most items to move.") final int count) {
        final Direction sourceDirection = requireDirection(sourceSide);
        final Direction targetDirection = requireDirection(targetSide);
        final ItemHandler source = requireItemHandler(sourceDirection, sourceSide);
        final ItemHandler target = requireItemHandler(targetDirection, targetSide);
        ItemHandlerProtocol.requireValidSlot(source, sourceSlot);
        ItemHandlerProtocol.requireValidSlot(target, targetSlot);

        if (count <= 0 || (sourceDirection == targetDirection && sourceSlot == targetSlot)) {
            return 0;
        }

        final ItemStack available = source.extractItem(sourceSlot, count, true);
        if (available.isEmpty()) {
            return 0;
        }

        final int accepted = available.getCount() - target.insertItem(targetSlot, available, true).getCount();
        if (accepted <= 0) {
            return 0;
        }

        final ItemStack moved = source.extractItem(sourceSlot, accepted, false);
        final ItemStack rejected = target.insertItem(targetSlot, moved, false);
        if (!rejected.isEmpty()) {
            returnToSource(source, sourceSlot, rejected);
        }

        return moved.getCount() - rejected.getCount();
    }

    @Callback(description = "Gets how many tanks the fluid container on the specified side has.",
        returnValueDescription = "the number of tanks, or `0` if there is no fluid container on that side.")
    public int getFluidTankCount(@Parameter(value = "side", description = SIDE_OF_CONTAINER) @Nullable final Side side) {
        final FluidHandler handler = getFluidHandler(requireDirection(side));
        return handler != null ? handler.getTanks() : 0;
    }

    @Callback(description = "Gets what is in the specified tank of the container on the specified side.",
        returnValueDescription = "a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.")
    public FluidStack getFluidInTank(@Parameter(value = "side", description = SIDE_OF_CONTAINER) @Nullable final Side side,
                                     @Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank) {
        final FluidHandler handler = requireFluidHandler(side);
        return handler.getFluidInTank(FluidHandlerProtocol.requireValidTank(handler, tank));
    }

    @Callback(description = "Gets how much the specified tank of the container on the specified side can hold.",
        returnValueDescription = "the capacity in millibuckets.")
    public int getFluidTankCapacity(@Parameter(value = "side", description = SIDE_OF_CONTAINER) @Nullable final Side side,
                                    @Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank) {
        final FluidHandler handler = requireFluidHandler(side);
        return handler.getTankCapacity(FluidHandlerProtocol.requireValidTank(handler, tank));
    }

    @Callback(description = "Moves fluid from one container to another. It moves as much as the source yields and the target accepts, up to `amount`. A bucket is 1000.",
        returnValueDescription = "the amount transferred in millibuckets.")
    public int moveFluid(@Parameter(value = "sourceSide", description = SIDE_TO_DRAIN) @Nullable final Side sourceSide,
                         @Parameter(value = "targetSide", description = SIDE_TO_FILL) @Nullable final Side targetSide,
                         @Parameter(value = "amount", description = "the most millibuckets to move.") final int amount) {
        final Direction sourceDirection = requireDirection(sourceSide);
        final Direction targetDirection = requireDirection(targetSide);
        final FluidHandler source = requireFluidHandler(sourceDirection, sourceSide);
        final FluidHandler target = requireFluidHandler(targetDirection, targetSide);

        if (amount <= 0 || sourceDirection == targetDirection) {
            return 0;
        }

        final FluidStack available = source.drain(amount, true);
        if (available.isEmpty()) {
            return 0;
        }

        final int accepted = target.fill(available, true);
        if (accepted <= 0) {
            return 0;
        }

        final FluidStack moved = source.drain(available.withAmount(accepted), false);
        if (moved.isEmpty()) {
            return 0;
        }

        final int filled = target.fill(moved, false);
        if (filled < moved.amount()) {
            source.fill(moved.withAmount(moved.amount() - filled), false);
        }

        return filled;
    }

    @Callback(description = "Drops items from an inventory into the world. The target side must not be blocked.",
        returnValueDescription = ITEMS_TRANSFERRED)
    public int dropItems(@Parameter(value = "sourceSide", description = SIDE_TO_TAKE_ITEMS_FROM) @Nullable final Side sourceSide,
                         @Parameter(value = "sourceSlot", description = SLOT_TO_TAKE_ITEMS_FROM) final int sourceSlot,
                         @Parameter(value = "targetSide", description = "the side to drop the items on.") @Nullable final Side targetSide,
                         @Parameter(value = "count", description = "the most items to drop.") final int count) {
        final ItemHandler source = requireItemHandler(sourceSide);
        final BlockPos targetPos = requireWorldAccess(requireDirection(targetSide));
        ItemHandlerProtocol.requireValidSlot(source, sourceSlot);
        if (!level.getBlockState(targetPos).getCollisionShape(level, targetPos).isEmpty()) {
            throw new IllegalArgumentException("side is obstructed: " + targetSide);
        }

        if (count <= 0) {
            return 0;
        }

        final ItemStack stack = source.extractItem(sourceSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final ItemEntity entity = new ItemEntity(level, targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, stack, 0, 0, 0);
        entity.setDefaultPickUpDelay();
        if (!level.addFreshEntity(entity)) {
            returnToSource(source, sourceSlot, stack);
            return 0;
        }

        return stack.getCount();
    }

    @Callback(description = "Picks up items lying in the world into an inventory. It takes as many items as the target slot accepts, up to `count`.",
        returnValueDescription = ITEMS_TRANSFERRED)
    public int takeItems(@Parameter(value = "sourceSide", description = "the side to pick items up from.") @Nullable final Side sourceSide,
                         @Parameter(value = "targetSide", description = SIDE_TO_PUT_ITEMS_INTO) @Nullable final Side targetSide,
                         @Parameter(value = "targetSlot", description = SLOT_TO_PUT_ITEMS_INTO) final int targetSlot,
                         @Parameter(value = "count", description = "the most items to take.") final int count) {
        final BlockPos sourcePos = requireWorldAccess(requireDirection(sourceSide));
        final ItemHandler target = requireItemHandler(targetSide);
        ItemHandlerProtocol.requireValidSlot(target, targetSlot);

        if (count <= 0) {
            return 0;
        }

        int remaining = count;
        for (final ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, new AABB(sourcePos))) {
            if (remaining <= 0) {
                break;
            }

            final ItemStack available = entity.getItem().copyWithCount(Math.min(entity.getItem().getCount(), remaining));
            final int accepted = available.getCount() - target.insertItem(targetSlot, available, true).getCount();
            if (accepted <= 0) {
                continue;
            }

            final ItemStack rejected = target.insertItem(targetSlot, available.copyWithCount(accepted), false);
            final int taken = accepted - rejected.getCount();
            remaining -= taken;

            final ItemStack left = entity.getItem().copy();
            left.shrink(taken);
            entity.setItem(left);
        }

        return count - remaining;
    }

    @Callback(description = "Pours one bucket of fluid from a container into the world. Nothing is transferred if the container holds less than a bucket, or the fluid cannot go there. Water placed in the Nether evaporates and still counts as placed.",
        returnValueDescription = BUCKET_OR_NOTHING)
    public int fillFluid(@Parameter(value = "sourceSide", description = SIDE_TO_DRAIN) @Nullable final Side sourceSide,
                         @Parameter(value = "targetSide", description = "the side to place the fluid on.") @Nullable final Side targetSide) {
        final FluidHandler source = requireFluidHandler(sourceSide);
        final BlockPos targetPos = requireWorldAccess(requireDirection(targetSide));
        if (level.getFluidState(targetPos).isSource()) {
            return 0;
        }

        final FluidStack available = source.drain(FluidHandler.BUCKET, true);
        if (available.amount() < FluidHandler.BUCKET || !(available.fluid().getBucket() instanceof final BucketItem bucket)) {
            return 0;
        }

        final FluidStack drained = source.drain(available, false);
        if (drained.amount() < FluidHandler.BUCKET) {
            source.fill(drained, false);
            return 0;
        }

        if (!bucket.emptyContents(null, level, targetPos, null)) {
            source.fill(drained, false);
            return 0;
        }

        return FluidHandler.BUCKET;
    }

    @Callback(description = "Drains a fluid source block, or out of a waterlogged block, into a container. Nothing is transferred if the container cannot hold a full bucket of it.",
        returnValueDescription = BUCKET_OR_NOTHING)
    public int drainFluid(@Parameter(value = "sourceSide", description = "the side to take the fluid from.") @Nullable final Side sourceSide,
                          @Parameter(value = "targetSide", description = SIDE_TO_FILL) @Nullable final Side targetSide) {
        final BlockPos sourcePos = requireWorldAccess(requireDirection(sourceSide));
        final FluidHandler target = requireFluidHandler(targetSide);

        final BlockState state = level.getBlockState(sourcePos);
        final FluidState fluidState = state.getFluidState();
        if (!fluidState.isSource() || !(fluidState.getType().getBucket() instanceof BucketItem)
            || !(state.getBlock() instanceof final BucketPickup pickup)) {
            return 0;
        }

        final FluidStack stack = new FluidStack(fluidState.getType(), FluidHandler.BUCKET);
        if (target.fill(stack, true) < FluidHandler.BUCKET) {
            return 0;
        }

        if (pickup.pickupBlock(null, level, sourcePos, state).isEmpty()) {
            return 0;
        }

        level.gameEvent(null, GameEvent.FLUID_PICKUP, sourcePos);
        return target.fill(stack, false);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ITEM_SLOT_COUNT_CODE, name = "getItemSlotCount",
        description = "Reads how many slots the inventory on that side has.",
        argumentsDescription = "one byte, the side.",
        resultsDescription = "one byte, the slot count, at most 255. A side without an inventory reads as 0.")
    public void getItemSlotCountIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getItemSlotCount(Side.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(value = GET_ITEM_SLOTS_CODE, name = "getSlots",
        description = "Reads a run of slots of the inventory on that side.",
        argumentsDescription = "three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.",
        resultsDescription = "four bytes per slot: the item as two bytes, the number of items up to 255, and damage.")
    public void getItemSlotsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlots(requireItemHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(value = GET_ITEM_SLOT_LIMIT_CODE, name = "getItemSlotLimit",
        description = "Reads how much a slot of the inventory on that side can hold.",
        argumentsDescription = "two bytes, the side and the slot.",
        resultsDescription = "one byte, the limit, at most 255.")
    public void getItemSlotLimitIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotLimit(requireItemHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(value = GET_ITEM_NAME_CODE, synchronize = false, name = "getItemName",
        description = "Reads the name of an item.",
        argumentsDescription = "two bytes, the item id.",
        resultsDescription = "the name, such as `minecraft:redstone`. Read while `OCDAV` is set to get all of it.")
    public void getItemNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemName(arguments, results);
    }

    @IOCallback(value = GET_ITEM_ID_CODE, synchronize = false, name = "getItemId",
        description = "Looks an item up by name.",
        argumentsDescription = "the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.",
        resultsDescription = "two bytes, the item id.")
    public void getItemIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemId(arguments, results);
    }

    @IOCallback(value = MOVE_ITEMS_CODE, name = "moveItems",
        description = "Moves up to `count` items between two slots.",
        argumentsDescription = "five bytes, the side and slot to take from, the side and slot to put into, and how many items to move at most.",
        resultsDescription = "one byte, how many items were moved.")
    public void moveItemsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final int sourceSlot = arguments.readU8();
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int targetSlot = arguments.readU8();
        final int count = arguments.readU8();
        results.writeU8(moveItems(sourceSide, sourceSlot, targetSide, targetSlot, count));
    }

    @IOCallback(value = GET_FLUID_TANK_COUNT_CODE, name = "getFluidTankCount",
        description = "Reads how many tanks the container on that side has.",
        argumentsDescription = "one byte, the side.",
        resultsDescription = "one byte, the tank count, at most 255. A side without a fluid container reads as 0.")
    public void getFluidTankCountIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getFluidTankCount(Side.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(value = GET_FLUID_TANKS_CODE, name = "getTanks",
        description = "Reads a run of tanks of the container on that side.",
        argumentsDescription = "three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.",
        resultsDescription = "six bytes per tank: the fluid as two bytes and the amount as four bytes.")
    public void getFluidTanksIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(requireFluidHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(value = GET_FLUID_TANK_CAPACITY_CODE, name = "getFluidTankCapacity",
        description = "Reads how much a tank of the container on that side can hold.",
        argumentsDescription = "two bytes, the side and the tank.",
        resultsDescription = "four bytes, the capacity.")
    public void getFluidTankCapacityIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(requireFluidHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(value = GET_FLUID_NAME_CODE, synchronize = false, name = "getFluidName",
        description = "Reads the name of a fluid.",
        argumentsDescription = "two bytes, the fluid id.",
        resultsDescription = "the name, such as `minecraft:water`. Read while `OCDAV` is set to get all of it.")
    public void getFluidNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidName(arguments, results);
    }

    @IOCallback(value = GET_FLUID_ID_CODE, synchronize = false, name = "getFluidId",
        description = "Looks a fluid up by name.",
        argumentsDescription = "the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.",
        resultsDescription = "two bytes, the fluid id.")
    public void getFluidIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidId(arguments, results);
    }

    @IOCallback(value = MOVE_FLUID_CODE, name = "moveFluid",
        description = "Moves up to `amount` millibuckets between two containers.",
        argumentsDescription = "six bytes, the side to drain, the side to fill, and four bytes for how much to move at most.",
        resultsDescription = "four bytes, how much was moved.")
    public void moveFluidIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int amount = (int) Math.min(arguments.readU32(), Integer.MAX_VALUE);
        results.writeU32(moveFluid(sourceSide, targetSide, amount));
    }

    @IOCallback(value = DROP_ITEMS_CODE, name = "dropItems",
        description = "Drops up to `count` items from a slot into the world.",
        argumentsDescription = "four bytes, the side and slot to take from, the side to drop on, and how many items to drop at most.",
        resultsDescription = "one byte, how many items were dropped.")
    public void dropItemsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final int sourceSlot = arguments.readU8();
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int count = arguments.readU8();
        results.writeU8(dropItems(sourceSide, sourceSlot, targetSide, count));
    }

    @IOCallback(value = TAKE_ITEMS_CODE, name = "takeItems",
        description = "Picks up to `count` items from the world into a slot.",
        argumentsDescription = "four bytes, the side to pick up from, the side and slot to put into, and how many items to take at most.",
        resultsDescription = "one byte, how many items were taken.")
    public void takeItemsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int targetSlot = arguments.readU8();
        final int count = arguments.readU8();
        results.writeU8(takeItems(sourceSide, targetSide, targetSlot, count));
    }

    @IOCallback(value = FILL_FLUID_CODE, name = "fillFluid",
        description = "Places one bucket of fluid from a container into the world.",
        argumentsDescription = "two bytes, the side to drain and the side to place on.",
        resultsDescription = "four bytes, how much was placed, 1000 or 0.")
    public void fillFluidIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final Side targetSide = Side.byIndex(arguments.readU8());
        results.writeU32(fillFluid(sourceSide, targetSide));
    }

    @IOCallback(value = DRAIN_FLUID_CODE, name = "drainFluid",
        description = "Takes a fluid block from the world into a container.",
        argumentsDescription = "two bytes, the side to take from and the side to fill.",
        resultsDescription = "four bytes, how much was taken, 1000 or 0.")
    public void drainFluidIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final Side targetSide = Side.byIndex(arguments.readU8());
        results.writeU32(drainFluid(sourceSide, targetSide));
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

    private BlockPos requireWorldAccess(final Direction direction) {
        final BlockPos pos = getBlockPos().relative(direction);
        if (!(level instanceof final ServerLevel serverLevel) || !serverLevel.isLoaded(pos)) {
            throw new IllegalStateException("not loaded");
        }
        if (!serverLevel.mayInteract(FakePlayerUtils.getFakePlayer(serverLevel), pos)) {
            throw new IllegalStateException("not allowed");
        }
        return pos;
    }

    private ItemHandler requireItemHandler(@Nullable final Side side) {
        return requireItemHandler(requireDirection(side), side);
    }

    private ItemHandler requireItemHandler(final Direction direction, @Nullable final Side side) {
        final ItemHandler handler = getItemHandler(direction);
        if (handler == null) {
            throw new IllegalArgumentException("no inventory on side: " + side);
        }
        return handler;
    }

    @Nullable
    private ItemHandler getItemHandler(final Direction direction) {
        final BlockPos neighborPos = getBlockPos().relative(direction);
        if (level == null || !level.isLoaded(neighborPos)) {
            return null;
        }

        final BlockEntity blockEntity = level.getBlockEntity(neighborPos);
        if (blockEntity == null) {
            return null;
        }

        return Capabilities.get(blockEntity, Capabilities.ITEM_HANDLER, direction.getOpposite());
    }

    private void returnToSource(final ItemHandler source, final int sourceSlot, final ItemStack stack) {
        final ItemStack lost = source.insertItem(sourceSlot, stack, false);
        if (!lost.isEmpty() && level != null) {
            final BlockPos pos = getBlockPos();
            Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), lost);
        }
    }

    private FluidHandler requireFluidHandler(@Nullable final Side side) {
        return requireFluidHandler(requireDirection(side), side);
    }

    private FluidHandler requireFluidHandler(final Direction direction, @Nullable final Side side) {
        final FluidHandler handler = getFluidHandler(direction);
        if (handler == null) {
            throw new IllegalArgumentException("no fluid container on side: " + side);
        }
        return handler;
    }

    @Nullable
    private FluidHandler getFluidHandler(final Direction direction) {
        final BlockPos neighborPos = getBlockPos().relative(direction);
        if (level == null || !level.isLoaded(neighborPos)) {
            return null;
        }

        return Capabilities.get(level, neighborPos, Capabilities.FLUID_HANDLER, direction.getOpposite());
    }
}

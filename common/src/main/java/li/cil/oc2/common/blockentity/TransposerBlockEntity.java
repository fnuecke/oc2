/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.blockentity;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.DocumentedDevice;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.util.Side;
import li.cil.oc2.common.bus.device.util.FluidHandlerProtocol;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.util.HorizontalBlockUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Collection;

import static java.util.Collections.singletonList;

@IOName("TRANSP")
public final class TransposerBlockEntity extends ModBlockEntity implements NamedDevice, DocumentedDevice {
    private static final String GET_ITEM_SLOT_COUNT = "getItemSlotCount";
    private static final String GET_ITEM_STACK_IN_SLOT = "getItemStackInSlot";
    private static final String GET_ITEM_SLOT_LIMIT = "getItemSlotLimit";
    private static final String MOVE_ITEMS = "moveItems";
    private static final String GET_FLUID_TANK_COUNT = "getFluidTankCount";
    private static final String GET_FLUID_IN_TANK = "getFluidInTank";
    private static final String GET_FLUID_TANK_CAPACITY = "getFluidTankCapacity";
    private static final String MOVE_FLUID = "moveFluid";

    private static final String SIDE = "side";
    private static final String SLOT = "slot";
    private static final String TANK = "tank";
    private static final String SOURCE_SIDE = "sourceSide";
    private static final String SOURCE_SLOT = "sourceSlot";
    private static final String TARGET_SIDE = "targetSide";
    private static final String TARGET_SLOT = "targetSlot";
    private static final String COUNT = "count";
    private static final String AMOUNT = "amount";

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

    // --------------------------------------------------------------------- //

    public TransposerBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.TRANSPOSER.get(), pos, state);
    }

    // --------------------------------------------------------------------- //

    @Callback(name = GET_ITEM_SLOT_COUNT)
    public int getItemSlotCount(@Parameter(SIDE) @Nullable final Side side) {
        final ItemHandler handler = getItemHandler(requireDirection(side));
        return handler != null ? handler.getSlots() : 0;
    }

    @Callback(name = GET_ITEM_STACK_IN_SLOT)
    public ItemStack getItemStackInSlot(@Parameter(SIDE) @Nullable final Side side, @Parameter(SLOT) final int slot) {
        final ItemHandler handler = requireItemHandler(side);
        return handler.getStackInSlot(ItemHandlerProtocol.requireValidSlot(handler, slot));
    }

    @Callback(name = GET_ITEM_SLOT_LIMIT)
    public int getItemSlotLimit(@Parameter(SIDE) @Nullable final Side side, @Parameter(SLOT) final int slot) {
        final ItemHandler handler = requireItemHandler(side);
        return handler.getSlotLimit(ItemHandlerProtocol.requireValidSlot(handler, slot));
    }

    @Callback(name = MOVE_ITEMS)
    public int moveItems(@Parameter(SOURCE_SIDE) @Nullable final Side sourceSide, @Parameter(SOURCE_SLOT) final int sourceSlot,
                         @Parameter(TARGET_SIDE) @Nullable final Side targetSide, @Parameter(TARGET_SLOT) final int targetSlot,
                         @Parameter(COUNT) final int count) {
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

    @Callback(name = GET_FLUID_TANK_COUNT)
    public int getFluidTankCount(@Parameter(SIDE) @Nullable final Side side) {
        final FluidHandler handler = getFluidHandler(requireDirection(side));
        return handler != null ? handler.getTanks() : 0;
    }

    @Callback(name = GET_FLUID_IN_TANK)
    public FluidStack getFluidInTank(@Parameter(SIDE) @Nullable final Side side, @Parameter(TANK) final int tank) {
        final FluidHandler handler = requireFluidHandler(side);
        return handler.getFluidInTank(FluidHandlerProtocol.requireValidTank(handler, tank));
    }

    @Callback(name = GET_FLUID_TANK_CAPACITY)
    public int getFluidTankCapacity(@Parameter(SIDE) @Nullable final Side side, @Parameter(TANK) final int tank) {
        final FluidHandler handler = requireFluidHandler(side);
        return handler.getTankCapacity(FluidHandlerProtocol.requireValidTank(handler, tank));
    }

    @Callback(name = MOVE_FLUID)
    public int moveFluid(@Parameter(SOURCE_SIDE) @Nullable final Side sourceSide,
                         @Parameter(TARGET_SIDE) @Nullable final Side targetSide,
                         @Parameter(AMOUNT) final int amount) {
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

    @Override
    public Collection<String> getDeviceTypeNames() {
        return singletonList("transposer");
    }

    @Override
    public void getDeviceDocumentation(final DeviceVisitor visitor) {
        final String sides = "\nSides may be specified by name or zero-based index: down, up, north, south, west, east.";

        visitor.visitCallback(GET_ITEM_SLOT_COUNT)
            .description("Get the number of slots of the inventory on the specified side." + sides)
            .returnValueDescription("the number of slots, zero if there is no inventory on that side.")
            .parameterDescription(SIDE, "the side of the inventory to inspect.");
        visitor.visitCallback(GET_ITEM_STACK_IN_SLOT)
            .description("Get the item stack in the specified slot of the inventory on the specified side." + sides)
            .returnValueDescription("the item stack in the slot, nothing if the slot is empty.")
            .parameterDescription(SIDE, "the side of the inventory to inspect.")
            .parameterDescription(SLOT, "the zero-based index of the slot to inspect.");
        visitor.visitCallback(GET_ITEM_SLOT_LIMIT)
            .description("Get the maximum number of items the specified slot of the inventory on the specified side can hold." + sides)
            .returnValueDescription("the maximum number of items the slot can hold.")
            .parameterDescription(SIDE, "the side of the inventory to inspect.")
            .parameterDescription(SLOT, "the zero-based index of the slot to inspect.");
        visitor.visitCallback(MOVE_ITEMS)
            .description("Move items from a slot of the inventory on one side to a slot of the inventory on another side. " +
                "Moves as many items as the source slot yields and the target slot accepts, up to the specified count." + sides)
            .returnValueDescription("the number of items moved.")
            .parameterDescription(SOURCE_SIDE, "the side of the inventory to take items from.")
            .parameterDescription(SOURCE_SLOT, "the zero-based index of the slot to take items from.")
            .parameterDescription(TARGET_SIDE, "the side of the inventory to put items into.")
            .parameterDescription(TARGET_SLOT, "the zero-based index of the slot to put items into.")
            .parameterDescription(COUNT, "the maximum number of items to move.");
        visitor.visitCallback(GET_FLUID_TANK_COUNT)
            .description("Get the number of tanks of the fluid container on the specified side." + sides)
            .returnValueDescription("the number of tanks, zero if there is no fluid container on that side.")
            .parameterDescription(SIDE, "the side of the fluid container to inspect.");
        visitor.visitCallback(GET_FLUID_IN_TANK)
            .description("Get the fluid in the specified tank of the fluid container on the specified side." + sides)
            .returnValueDescription("the fluid and its amount in millibuckets, nothing if the tank is empty.")
            .parameterDescription(SIDE, "the side of the fluid container to inspect.")
            .parameterDescription(TANK, "the zero-based index of the tank to inspect.");
        visitor.visitCallback(GET_FLUID_TANK_CAPACITY)
            .description("Get the capacity of the specified tank of the fluid container on the specified side." + sides)
            .returnValueDescription("the capacity of the tank in millibuckets.")
            .parameterDescription(SIDE, "the side of the fluid container to inspect.")
            .parameterDescription(TANK, "the zero-based index of the tank to inspect.");
        visitor.visitCallback(MOVE_FLUID)
            .description("Move fluid from the fluid container on one side to the fluid container on another side. " +
                "Moves as much as the source yields and the target accepts, up to the specified amount." + sides)
            .returnValueDescription("the amount moved in millibuckets.")
            .parameterDescription(SOURCE_SIDE, "the side of the fluid container to drain.")
            .parameterDescription(TARGET_SIDE, "the side of the fluid container to fill.")
            .parameterDescription(AMOUNT, "the maximum amount to move in millibuckets.");
    }

    // --------------------------------------------------------------------- //

    @IOCallback(GET_ITEM_SLOT_COUNT_CODE)
    public void getItemSlotCountIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getItemSlotCount(Side.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(GET_ITEM_SLOTS_CODE)
    public void getItemSlotsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlots(requireItemHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(GET_ITEM_SLOT_LIMIT_CODE)
    public void getItemSlotLimitIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotLimit(requireItemHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(value = GET_ITEM_NAME_CODE, synchronize = false)
    public void getItemNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemName(arguments, results);
    }

    @IOCallback(value = GET_ITEM_ID_CODE, synchronize = false)
    public void getItemIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeItemId(arguments, results);
    }

    @IOCallback(MOVE_ITEMS_CODE)
    public void moveItemsIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final Side sourceSide = Side.byIndex(arguments.readU8());
        final int sourceSlot = arguments.readU8();
        final Side targetSide = Side.byIndex(arguments.readU8());
        final int targetSlot = arguments.readU8();
        final int count = arguments.readU8();
        results.writeU8(moveItems(sourceSide, sourceSlot, targetSide, targetSlot, count));
    }

    @IOCallback(GET_FLUID_TANK_COUNT_CODE)
    public void getFluidTankCountIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getFluidTankCount(Side.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(GET_FLUID_TANKS_CODE)
    public void getFluidTanksIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(requireFluidHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(GET_FLUID_TANK_CAPACITY_CODE)
    public void getFluidTankCapacityIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(requireFluidHandler(Side.byIndex(arguments.readU8())), arguments, results);
    }

    @IOCallback(value = GET_FLUID_NAME_CODE, synchronize = false)
    public void getFluidNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidName(arguments, results);
    }

    @IOCallback(value = GET_FLUID_ID_CODE, synchronize = false)
    public void getFluidIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidId(arguments, results);
    }

    @IOCallback(MOVE_FLUID_CODE)
    public void moveFluidIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
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

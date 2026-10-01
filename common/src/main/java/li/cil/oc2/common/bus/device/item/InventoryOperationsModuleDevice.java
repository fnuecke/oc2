/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.util.ItemHandlerDeviceUtils;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ItemHandlerUtils;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.util.FakePlayerUtils;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

@RPCDeviceDescription(typeNames = {"inventory_operations"}, description = """
    Provided by the [inventory operations module](../item/inventory_operations_module.md) to robots.

    The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

    Allows operating the internal inventory, as well as inspecting and operating block and entity containers. If neither external container type is present, space permitting, items are dropped into the world or picked up.

    Sides without a container report no inventory, which allows predicting world interaction, e.g. to ensure things are never dumped into the world.""")
@IODeviceDescription(name = "INVOPS", description = """
    Sides are numbered: `0` front, `1` up and `2` down.""")
public final class InventoryOperationsModuleDevice extends AbstractItemDevice {
    private static final int MOVE_CODE = 1;
    private static final int DROP_CODE = 2;
    private static final int DROP_INTO_CODE = 3;
    private static final int TAKE_CODE = 4;
    private static final int TAKE_FROM_CODE = 5;
    private static final int MOVE_INTO_CODE = 6;
    private static final int MOVE_FROM_CODE = 7;
    private static final int GET_ITEM_SLOT_COUNT_CODE = 8;
    private static final int GET_ITEM_SLOTS_CODE = 9;
    private static final int GET_ITEM_SLOT_LIMIT_CODE = 10;

    private static final String DROP_DESCRIPTION = "Tries to drop items from the selected slot in the specified direction. Items are " +
        "dropped into an inventory, or into the world if no inventory is present.";
    private static final String DROP_INTO_DESCRIPTION = "Tries to drop items from the selected slot into the specified slot of an inventory " +
        "in the specified direction. Items are only dropped into an inventory, never into the world.";
    private static final String TAKE_DESCRIPTION = "Tries to take the specified number of items from the specified direction. Items are " +
        "taken from an inventory, or from the world if no inventory is present.";
    private static final String TAKE_FROM_DESCRIPTION = "Tries to take the specified number of items from the specified slot of an inventory " +
        "in the specified direction. Items are only taken from an inventory, never from the world.";
    private static final String MOVE_INTO_DESCRIPTION = "Tries to move items from the specified robot inventory slot into a " +
        "container item, such as a shulker box, in the selected slot.";
    private static final String MOVE_FROM_DESCRIPTION = "Tries to move items out of a container item in the selected slot " +
        "into the specified robot inventory slot.";

    // --------------------------------------------------------------------- //

    private final Entity entity;
    private final Robot robot;

    // --------------------------------------------------------------------- //

    public InventoryOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity);
        this.entity = entity;
        this.robot = robot;
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many slots the inventory on the specified side has.",
        returnValueDescription = "the number of slots, or `0` if there is no inventory on that side.")
    public int getItemSlotCount() {
        return getItemSlotCount(null);
    }

    @Callback(description = "Gets how many slots the inventory on the specified side has.",
        returnValueDescription = "the number of slots, or `0` if there is no inventory on that side.")
    public int getItemSlotCount(@Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        final List<ItemHandler> handlers = getItemHandlers(side);
        return handlers.isEmpty() ? 0 : handlers.getFirst().getSlots();
    }

    @Callback(description = "Gets what is in the specified slot of the inventory on the specified side.",
        returnValueDescription = "a table with the item information. Returns nothing for an empty slot.")
    public ItemStack getItemStackInSlot(@Parameter(value = "slot", description = "the slot to inspect.") final int slot) {
        return getItemStackInSlot(slot, null);
    }

    @Callback(description = "Gets what is in the specified slot of the inventory on the specified side.",
        returnValueDescription = "a table with the item information. Returns nothing for an empty slot.")
    public ItemStack getItemStackInSlot(@Parameter(value = "slot", description = "the slot to inspect.") final int slot,
                                        @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        final ItemHandler handler = requireItemHandlers(side).getFirst();
        return handler.getStackInSlot(ItemHandlerDeviceUtils.requireValidSlot(handler, slot));
    }

    @Callback(description = "Gets how many items the specified slot of the inventory on the specified side can hold.",
        returnValueDescription = "the most items the slot takes.")
    public int getItemSlotLimit(@Parameter(value = "slot", description = "the slot to inspect.") final int slot) {
        return getItemSlotLimit(slot, null);
    }

    @Callback(description = "Gets how many items the specified slot of the inventory on the specified side can hold.",
        returnValueDescription = "the most items the slot takes.")
    public int getItemSlotLimit(@Parameter(value = "slot", description = "the slot to inspect.") final int slot,
                                @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        final ItemHandler handler = requireItemHandlers(side).getFirst();
        return handler.getSlotLimit(ItemHandlerDeviceUtils.requireValidSlot(handler, slot));
    }

    @Callback(energy = 1, description = "Tries to move the specified number of items from one robot inventory slot to another.",
        returnValueDescription = "the number of items moved.")
    public int move(@Parameter(value = "fromSlot", description = "the slot to extract items from.") final int fromSlot,
                    @Parameter(value = "intoSlot", description = "the slot to insert items into.") final int intoSlot,
                    @Parameter(value = "count", description = "the number of items to move.") final int count) {
        final ItemHandler inventory = inventory();
        ItemHandlerDeviceUtils.requireValidSlot(inventory, fromSlot);
        ItemHandlerDeviceUtils.requireValidSlot(inventory, intoSlot);
        if (fromSlot == intoSlot) {
            return 0;
        }

        return ItemHandlerUtils.transfer(inventory, fromSlot, count,
            (stack, simulate) -> inventory.insertItem(intoSlot, stack, simulate), entity::spawnAtLocation);
    }

    @Callback(energy = 1, description = DROP_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int drop(@Parameter(value = "count", description = "the number of items to drop.") final int count) {
        return drop(count, null);
    }

    @Callback(energy = 1, description = DROP_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int drop(@Parameter(value = "count", description = "the number of items to drop.") final int count,
                    @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        return ItemHandlerUtils.transferFirst(List.of(inventory()), robot.getSelectedSlot(),
            requireItemHandlersOrWorld(side), ItemHandlerUtils.ANY_SLOT, 0, count, entity::spawnAtLocation);
    }

    @Callback(energy = 1, description = DROP_INTO_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int dropInto(@Parameter(value = "intoSlot", description = "the slot to insert items into.") final int intoSlot,
                        @Parameter(value = "count", description = "the number of items to drop.") final int count) {
        return dropInto(intoSlot, count, null);
    }

    @Callback(energy = 1, description = DROP_INTO_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int dropInto(@Parameter(value = "intoSlot", description = "the slot to insert items into.") final int intoSlot,
                        @Parameter(value = "count", description = "the number of items to drop.") final int count,
                        @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        return ItemHandlerUtils.transferFirst(List.of(inventory()), robot.getSelectedSlot(),
            ItemHandlerDeviceUtils.requireItemHandlersWithSlot(requireItemHandlers(side), intoSlot), intoSlot, 0, count, entity::spawnAtLocation);
    }

    @Callback(energy = 1, description = TAKE_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int take(@Parameter(value = "count", description = "the number of items to take.") final int count) {
        return take(count, null);
    }

    @Callback(energy = 1, description = TAKE_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int take(@Parameter(value = "count", description = "the number of items to take.") final int count,
                    @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        return ItemHandlerUtils.transferFirst(requireItemHandlersOrWorld(side), ItemHandlerUtils.ANY_SLOT,
            List.of(inventory()), ItemHandlerUtils.ANY_SLOT, robot.getSelectedSlot(), count, entity::spawnAtLocation);
    }

    @Callback(energy = 1, description = TAKE_FROM_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int takeFrom(@Parameter(value = "fromSlot", description = "the slot to take items from.") final int fromSlot,
                        @Parameter(value = "count", description = "the number of items to take.") final int count) {
        return takeFrom(fromSlot, count, null);
    }

    @Callback(energy = 1, description = TAKE_FROM_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int takeFrom(@Parameter(value = "fromSlot", description = "the slot to take items from.") final int fromSlot,
                        @Parameter(value = "count", description = "the number of items to take.") final int count,
                        @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        return ItemHandlerUtils.transferFirst(ItemHandlerDeviceUtils.requireItemHandlersWithSlot(requireItemHandlers(side), fromSlot), fromSlot,
            List.of(inventory()), ItemHandlerUtils.ANY_SLOT, robot.getSelectedSlot(), count, entity::spawnAtLocation);
    }

    @Callback(energy = 1, description = MOVE_INTO_DESCRIPTION, returnValueDescription = "the number of items moved.")
    public int moveInto(@Parameter(value = "fromSlot", description = "the slot to take items from.") final int fromSlot,
                        @Parameter(value = "count", description = "the number of items to move.") final int count) {
        final ItemHandler inventory = inventory();
        ItemHandlerDeviceUtils.requireValidSlot(inventory, fromSlot);
        final int selectedSlot = robot.getSelectedSlot();
        if (fromSlot == selectedSlot) {
            return 0;
        }

        return ItemHandlerUtils.transferFirst(List.of(inventory), fromSlot,
            List.of(ItemHandlerDeviceUtils.requireContainerItem(inventory, selectedSlot)), ItemHandlerUtils.ANY_SLOT, 0, count, entity::spawnAtLocation);
    }

    @Callback(energy = 1, description = MOVE_FROM_DESCRIPTION, returnValueDescription = "the number of items moved.")
    public int moveFrom(@Parameter(value = "intoSlot", description = "the slot to insert items into.") final int intoSlot,
                        @Parameter(value = "count", description = "the number of items to move.") final int count) {
        final ItemHandler inventory = inventory();
        ItemHandlerDeviceUtils.requireValidSlot(inventory, intoSlot);
        final int selectedSlot = robot.getSelectedSlot();
        if (intoSlot == selectedSlot) {
            return 0;
        }

        return ItemHandlerUtils.transferFirst(List.of(ItemHandlerDeviceUtils.requireContainerItem(inventory, selectedSlot)), ItemHandlerUtils.ANY_SLOT,
            List.of(inventory), intoSlot, 0, count, entity::spawnAtLocation);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_ITEM_SLOT_COUNT_CODE,
        description = "Reads how many slots the inventory on that side has.",
        argumentsDescription = "one byte, the side.",
        resultsDescription = "one byte, the slot count, at most 255. A side without an inventory reads as 0.")
    public void getItemSlotCount(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getItemSlotCount(RobotOperationSide.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(value = GET_ITEM_SLOTS_CODE,
        description = "Reads a run of slots of the inventory on that side.",
        argumentsDescription = "three bytes, the side, the slot to start at and how many slots to read, from 1 to 64.",
        resultsDescription = "four bytes per slot: the item as two bytes, the number of items up to 255, and damage.")
    public void getItemSlots(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlots(requireItemHandlers(RobotOperationSide.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(value = GET_ITEM_SLOT_LIMIT_CODE,
        description = "Reads how much a slot of the inventory on that side can hold.",
        argumentsDescription = "two bytes, the side and the slot.",
        resultsDescription = "one byte, the limit, at most 255.")
    public void getItemSlotLimit(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        ItemHandlerProtocol.writeSlotLimit(requireItemHandlers(RobotOperationSide.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(energy = 1, value = MOVE_CODE,
        description = "Tries to move the specified number of items from one robot inventory slot to another.",
        argumentsDescription = "three bytes, the slot to extract items from, the slot to insert items into, and the number of items to move.",
        resultsDescription = "one byte, the number of items moved.")
    public void move(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(move(arguments.readU8(), arguments.readU8(), arguments.readU8()));
    }

    @IOCallback(energy = 1, value = DROP_CODE,
        description = DROP_DESCRIPTION,
        argumentsDescription = "two bytes: the number of items to drop, and the side to drop them in.",
        resultsDescription = "one byte, the number of items dropped.")
    public void drop(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(drop(arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(energy = 1, value = DROP_INTO_CODE,
        description = DROP_INTO_DESCRIPTION,
        argumentsDescription = "three bytes: the slot to insert items into, the number of items to drop, and the side of the inventory.",
        resultsDescription = "one byte, the number of items dropped.")
    public void dropInto(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(dropInto(arguments.readU8(), arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(energy = 1, value = TAKE_CODE,
        description = TAKE_DESCRIPTION,
        argumentsDescription = "two bytes: the number of items to take, and the side to take them from.",
        resultsDescription = "one byte, the number of items taken.")
    public void take(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(take(arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(energy = 1, value = TAKE_FROM_CODE,
        description = TAKE_FROM_DESCRIPTION,
        argumentsDescription = "three bytes: the slot to take items from, the number of items to take, and the side of the inventory.",
        resultsDescription = "one byte, the number of items taken.")
    public void takeFrom(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(takeFrom(arguments.readU8(), arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(energy = 1, value = MOVE_INTO_CODE,
        description = MOVE_INTO_DESCRIPTION,
        argumentsDescription = "two bytes: the slot to extract items from, and the number of items to move.",
        resultsDescription = "one byte, the number of items moved.")
    public void moveInto(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(moveInto(arguments.readU8(), arguments.readU8()));
    }

    @IOCallback(energy = 1, value = MOVE_FROM_CODE,
        description = MOVE_FROM_DESCRIPTION,
        argumentsDescription = "two bytes: the slot to insert items into, and the number of items to move.",
        resultsDescription = "one byte, the number of items moved.")
    public void moveFrom(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(moveFrom(arguments.readU8(), arguments.readU8()));
    }

    // --------------------------------------------------------------------- //

    private List<ItemHandler> getItemHandlers(@Nullable final RobotOperationSide side) {
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return Capabilities.getAll((ServerLevel) entity.level(), entity.blockPosition().relative(direction),
            Capabilities.ITEM_HANDLER, direction.getOpposite(), entity);
    }

    private List<ItemHandler> requireItemHandlers(@Nullable final RobotOperationSide side) {
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return ItemHandlerDeviceUtils.requireItemHandlers((ServerLevel) entity.level(), entity.blockPosition().relative(direction),
            direction.getOpposite(), entity, side != null ? side : RobotOperationSide.FRONT);
    }

    private List<ItemHandler> requireItemHandlersOrWorld(@Nullable final RobotOperationSide side) {
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final ServerLevel level = (ServerLevel) entity.level();
        return ItemHandlerDeviceUtils.requireItemHandlersOrWorld(level, entity.blockPosition().relative(direction), direction.getOpposite(),
            entity, FakePlayerUtils.getFakePlayer(level, entity), side != null ? side : RobotOperationSide.FRONT);
    }

    private ItemHandler inventory() {
        return Objects.requireNonNull(Capabilities.get(entity, Capabilities.ITEM_HANDLER, null));
    }
}

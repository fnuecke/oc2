/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc.item;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.util.ItemHandlerProtocol;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.container.ItemHandlerUtils;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RPCDeviceDescription(typeNames = {"inventory_operations"}, description = """
    Provided by the [inventory operations module](../item/inventory_operations_module.md) to robots.

    ### Sides
    The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.""")
@IODeviceDescription(name = "INVOPS", description = """
    Sides are numbered: `0` front, `1` up and `2` down.""")
public final class InventoryOperationsModuleDevice extends AbstractItemRPCDevice {
    private static final int MOVE_CODE = 1;
    private static final int DROP_CODE = 2;
    private static final int DROP_INTO_CODE = 3;
    private static final int TAKE_CODE = 4;
    private static final int TAKE_FROM_CODE = 5;

    private static final String DROP_DESCRIPTION = "Tries to drop items from the selected slot in the specified direction. Items are " +
        "dropped into an inventory, or into the world if no inventory is present.";
    private static final String DROP_INTO_DESCRIPTION = "Tries to drop items from the selected slot into the specified slot of an inventory " +
        "in the specified direction. Items are only dropped into an inventory, never into the world.";
    private static final String TAKE_DESCRIPTION = "Tries to take the specified number of items from the specified direction. Items are " +
        "taken from an inventory, or from the world if no inventory is present.";
    private static final String TAKE_FROM_DESCRIPTION = "Tries to take the specified number of items from the specified slot of an inventory " +
        "in the specified direction. Items are only taken from an inventory, never from the world.";

    private final Entity entity;
    private final Robot robot;

    // --------------------------------------------------------------------- //

    public InventoryOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity);
        this.entity = entity;
        this.robot = robot;
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Tries to move the specified number of items from one robot inventory slot to another.")
    public void move(@Parameter(value = "fromSlot", description = "the slot to extract items from.") final int fromSlot,
                     @Parameter(value = "intoSlot", description = "the slot to insert items into.") final int intoSlot,
                     @Parameter(value = "count", description = "the number of items to move.") final int count) {
        if (count <= 0) {
            return;
        }

        final ItemHandler inventory = inventory();
        ItemHandlerProtocol.requireValidSlot(inventory, fromSlot);
        ItemHandlerProtocol.requireValidSlot(inventory, intoSlot);

        // Do simulation run, validating slot indices and getting actual amount possible to move.
        ItemStack extracted = inventory.extractItem(fromSlot, count, true);
        ItemStack remaining = inventory.insertItem(intoSlot, extracted, true);

        // Do actual run, move as many as we know we can, based on simulation.
        extracted = inventory.extractItem(fromSlot, extracted.getCount() - remaining.getCount(), false);
        remaining = inventory.insertItem(intoSlot, extracted, false);

        // But don't trust simulation; if something is remaining after actual run, try to put it back.
        remaining = inventory.insertItem(fromSlot, remaining, false);

        // And if putting it back fails, just drop it. Avoid destroying items.
        if (!remaining.isEmpty()) {
            entity.spawnAtLocation(remaining);
        }
    }

    @Callback(description = DROP_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int drop(@Parameter(value = "count", description = "the number of items to drop.") final int count) {
        return drop(count, null);
    }

    @Callback(description = DROP_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int drop(@Parameter(value = "count", description = "the number of items to drop.") final int count,
                    @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        ItemStack stack = inventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final List<ItemHandler> itemHandlers = getItemStackHandlersInDirection(direction).toList();
        for (final ItemHandler handler : itemHandlers) {
            stack = ItemHandlerUtils.insertItemStack(handler, stack, false);

            if (stack.isEmpty()) {
                break;
            }
        }

        // When we have items left, but there was an inventory, do *not* drop into the world.
        // Instead, try to put items back where they came from. Only failing that drop them
        // into the world.
        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty() && !itemHandlers.isEmpty()) {
            stack = inventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback(description = DROP_INTO_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int dropInto(@Parameter(value = "intoSlot", description = "the slot to insert the items into.") final int intoSlot,
                        @Parameter(value = "count", description = "the number of items to drop.") final int count) {
        return dropInto(intoSlot, count, null);
    }

    @Callback(description = DROP_INTO_DESCRIPTION, returnValueDescription = "the number of items dropped.")
    public int dropInto(@Parameter(value = "intoSlot", description = "the slot to insert the items into.") final int intoSlot,
                        @Parameter(value = "count", description = "the number of items to drop.") final int count,
                        @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final Optional<ItemHandler> optional = getItemStackHandlersInDirection(direction).findFirst();
        optional.ifPresent(handler -> ItemHandlerProtocol.requireValidSlot(handler, intoSlot));

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        ItemStack stack = inventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        if (optional.isPresent()) {
            stack = optional.get().insertItem(intoSlot, stack, false);
        }

        // Subtle difference to drop(), we always try to put the remainder back. This method
        // attempts to never drop anything into the world.
        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty()) {
            stack = inventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback(description = TAKE_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int take(@Parameter(value = "count", description = "the number of items to take.") final int count) {
        return take(count, null);
    }

    @Callback(description = TAKE_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int take(@Parameter(value = "count", description = "the number of items to take.") final int count,
                    @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final List<ItemHandler> handlers = getItemStackHandlersInDirection(direction).collect(Collectors.toList());
        if (handlers.isEmpty()) {
            return takeFromWorld(count);
        } else {
            return takeFromInventories(count, handlers);
        }
    }

    @Callback(description = TAKE_FROM_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int takeFrom(@Parameter(value = "fromSlot", description = "the slot to take the items from.") final int fromSlot,
                        @Parameter(value = "count", description = "the number of items to take.") final int count) {
        return takeFrom(fromSlot, count, null);
    }

    @Callback(description = TAKE_FROM_DESCRIPTION, returnValueDescription = "the number of items taken.")
    public int takeFrom(@Parameter(value = "fromSlot", description = "the slot to take the items from.") final int fromSlot,
                        @Parameter(value = "count", description = "the number of items to take.") final int count,
                        @Parameter(value = "side", description = "`front`, `up` or `down`. Optional, defaults to `front`.", optional = true) @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return getItemStackHandlersInDirection(direction).findFirst().map(handler ->
            takeFromInventory(count, handler, ItemHandlerProtocol.requireValidSlot(handler, fromSlot))).orElse(0);
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = MOVE_CODE,
        description = "Tries to move the specified number of items from one robot inventory slot to another.",
        argumentsDescription = "three bytes, the slot to extract items from, the slot to insert items into, and the number of items to move.")
    public void move(final IOInputStream arguments) throws IOException {
        move(arguments.readU8(), arguments.readU8(), arguments.readU8());
    }

    @IOCallback(value = DROP_CODE,
        description = DROP_DESCRIPTION,
        argumentsDescription = "two bytes: the number of items to drop, and the side to drop them in.",
        resultsDescription = "one byte, the number of items dropped.")
    public void drop(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(drop(arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(value = DROP_INTO_CODE,
        description = DROP_INTO_DESCRIPTION,
        argumentsDescription = "three bytes: the slot to insert the items into, the number of items to drop, and the side of the inventory.",
        resultsDescription = "one byte, the number of items dropped.")
    public void dropInto(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(dropInto(arguments.readU8(), arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(value = TAKE_CODE,
        description = TAKE_DESCRIPTION,
        argumentsDescription = "two bytes: the number of items to take, and the side to take them from.",
        resultsDescription = "one byte, the number of items taken.")
    public void take(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(take(arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(value = TAKE_FROM_CODE,
        description = TAKE_FROM_DESCRIPTION,
        argumentsDescription = "three bytes: the slot to take the items from, the number of items to take, and the side of the inventory.",
        resultsDescription = "one byte, the number of items taken.")
    public void takeFrom(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(takeFrom(arguments.readU8(), arguments.readU8(), RobotOperationSide.byIndex(arguments.readU8())));
    }

    // --------------------------------------------------------------------- //

    private ItemStack insertStartingAt(final ItemHandler handler, ItemStack stack, final int startSlot, final boolean simulate) {
        for (int i = 0; i < handler.getSlots(); i++) {
            final int slot = (startSlot + i) % handler.getSlots();
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private Stream<ItemHandler> getItemStackHandlersInDirection(final Direction direction) {
        return getItemStackHandlersAt(Vec3.atCenterOf(entity.blockPosition().relative(direction)), direction.getOpposite());
    }

    private Stream<ItemHandler> getItemStackHandlersAt(final Vec3 position, final Direction side) {
        return Stream.concat(getEntityItemHandlersAt(position, side), getBlockItemHandlersAt(position, side));
    }

    private Stream<ItemHandler> getEntityItemHandlersAt(final Vec3 position, final Direction side) {
        final AABB bounds = AABB.unitCubeFromLowerCorner(position.subtract(0.5, 0.5, 0.5));
        return entity.level().getEntities(entity, bounds).stream()
            .map(e -> Capabilities.get(e, Capabilities.ITEM_HANDLER, side))
            .filter(Objects::nonNull);
    }

    private Stream<ItemHandler> getBlockItemHandlersAt(final Vec3 position, final Direction side) {
        final BlockPos pos = BlockPos.containing(position);
        final BlockEntity blockEntity = entity.level().getBlockEntity(pos);
        if (blockEntity == null) {
            return Stream.empty();
        }

        final ItemHandler itemHandler = Capabilities.get(blockEntity, Capabilities.ITEM_HANDLER, side);
        return itemHandler != null ? Stream.of(itemHandler) : Stream.empty();
    }

    private List<ItemEntity> getItemsInRange() {
        return entity.level().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(1));
    }

    private int takeFromWorld(final int count) {
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemHandler inventory = inventory();

        int remaining = count;
        for (final ItemEntity itemEntity : getItemsInRange()) {
            // NB: We make a copy of the original so that the setItem at the end sends an update to the client.
            final ItemStack original = itemEntity.getItem().copy();

            final ItemStack stackToInsert = original.copy();
            if (stackToInsert.getCount() > remaining) {
                stackToInsert.setCount(remaining);
            }

            final ItemStack overflow = insertStartingAt(inventory, stackToInsert, selectedSlot, false);
            final int taken = stackToInsert.getCount() - overflow.getCount();

            remaining -= taken;
            original.shrink(taken);
            itemEntity.setItem(original);
        }

        return count - remaining;
    }

    private int takeFromInventories(final int count, final List<ItemHandler> handlers) {
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemHandler inventory = inventory();

        int remaining = count;
        for (final ItemHandler handler : handlers) {
            for (int fromSlot = 0; fromSlot < handler.getSlots(); fromSlot++) {
                // Do simulation run, getting actual amount possible to take.
                ItemStack extracted = handler.extractItem(fromSlot, remaining, true);
                ItemStack overflow = insertStartingAt(inventory, extracted, selectedSlot, true);

                final int delta = extracted.getCount() - overflow.getCount();
                if (delta == 0) {
                    continue;
                }

                remaining -= delta;

                // Do actual run, take as many as we know we can, based on simulation.
                extracted = handler.extractItem(fromSlot, delta, false);
                overflow = insertStartingAt(inventory, extracted, selectedSlot, false);

                // But don't trust simulation; if something is remaining after actual run, try to put it back.
                remaining += overflow.getCount();
                overflow = handler.insertItem(fromSlot, overflow, false);

                // And if putting it back fails, just drop it. Avoid destroying items.
                if (!overflow.isEmpty()) {
                    remaining -= overflow.getCount();
                    entity.spawnAtLocation(overflow);
                }
            }

            if (remaining <= 0) {
                break;
            }
        }

        return count - remaining;
    }

    private int takeFromInventory(final int count, final ItemHandler handler, final int slot) {
        final ItemHandler inventory = inventory();
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        // Do simulation run, getting actual amount possible to take.
        ItemStack extracted = handler.extractItem(slot, count, true);
        ItemStack overflow = insertStartingAt(inventory, extracted, selectedSlot, true);

        int taken = extracted.getCount() - overflow.getCount();

        // Do actual run, take as many as we know we can, based on simulation.
        extracted = handler.extractItem(slot, taken, false);
        overflow = insertStartingAt(inventory, extracted, selectedSlot, false);

        // But don't trust simulation; if something is remaining after actual run, try to put it back.
        taken -= overflow.getCount();
        overflow = handler.insertItem(slot, overflow, false);

        // And if putting it back fails, just drop it. Avoid destroying items.
        if (!overflow.isEmpty()) {
            // NB: not counting this towards taken count since it did not end up in our inventory.
            entity.spawnAtLocation(overflow);
        }

        return taken;
    }

    private ItemHandler inventory() {
        return Objects.requireNonNull(Capabilities.get(entity, Capabilities.ITEM_HANDLER, null));
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InventoryOperationsModuleDevice extends AbstractItemRPCDevice {
    private final Entity entity;
    private final Robot robot;

    ///////////////////////////////////////////////////////////////////

    public InventoryOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity, "inventory_operations");
        this.entity = entity;
        this.robot = robot;
    }

    ///////////////////////////////////////////////////////////////////

    @Callback
    public void move(@Parameter("fromSlot") final int fromSlot,
                     @Parameter("intoSlot") final int intoSlot,
                     @Parameter("count") final int count) {
        if (count <= 0) {
            return;
        }

        final ItemStackHandler inventory = robot.getInventory();

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

    @Callback
    public int drop(@Parameter("count") final int count) {
        return drop(count, null);
    }

    @Callback
    public int drop(@Parameter("count") final int count,
                    @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        ItemStack stack = robot.getInventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final List<IItemHandler> itemHandlers = getItemStackHandlersInDirection(direction).toList();
        for (final IItemHandler handler : itemHandlers) {
            stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);

            if (stack.isEmpty()) {
                break;
            }
        }

        // When we have items left, but there was an inventory, do *not* drop into the world.
        // Instead, try to put items back where they came from. Only failing that drop them
        // into the world.
        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty() && !itemHandlers.isEmpty()) {
            stack = robot.getInventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback
    public int dropInto(@Parameter("intoSlot") final int intoSlot,
                        @Parameter("count") final int count) {
        return dropInto(intoSlot, count, null);
    }

    @Callback
    public int dropInto(@Parameter("intoSlot") final int intoSlot,
                        @Parameter("count") final int count,
                        @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        ItemStack stack = robot.getInventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final Optional<IItemHandler> optional = getItemStackHandlersInDirection(direction).findFirst();
        if (optional.isPresent()) {
            stack = optional.get().insertItem(intoSlot, stack, false);
        }

        // Subtle difference to drop(), we always try to put the remainder back. This method
        // attempts to never drop anything into the world.
        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty()) {
            stack = robot.getInventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback
    public int take(@Parameter("count") final int count) {
        return take(count, null);
    }

    @Callback
    public int take(@Parameter("count") final int count,
                    @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final List<IItemHandler> handlers = getItemStackHandlersInDirection(direction).collect(Collectors.toList());
        if (handlers.isEmpty()) {
            return takeFromWorld(count);
        } else {
            return takeFromInventories(count, handlers);
        }
    }

    @Callback
    public int takeFrom(@Parameter("fromSlot") final int fromSlot,
                        @Parameter("count") final int count) {
        return takeFrom(fromSlot, count, null);
    }

    @Callback
    public int takeFrom(@Parameter("fromSlot") final int fromSlot,
                        @Parameter("count") final int count,
                        @Parameter("side") @Nullable final RobotOperationSide side) {
        if (count <= 0) {
            return 0;
        }

        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return getItemStackHandlersInDirection(direction).findFirst().map(handler ->
            takeFromInventory(count, handler, fromSlot)).orElse(0);
    }

    ///////////////////////////////////////////////////////////////////

    private ItemStack insertStartingAt(final IItemHandler handler, ItemStack stack, final int startSlot, final boolean simulate) {
        for (int i = 0; i < handler.getSlots(); i++) {
            final int slot = (startSlot + i) % handler.getSlots();
            stack = handler.insertItem(slot, stack, simulate);
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        return stack;
    }

    private Stream<IItemHandler> getItemStackHandlersInDirection(final Direction direction) {
        return getItemStackHandlersAt(Vec3.atCenterOf(entity.blockPosition().relative(direction)), direction.getOpposite());
    }

    private Stream<IItemHandler> getItemStackHandlersAt(final Vec3 position, final Direction side) {
        return Stream.concat(getEntityItemHandlersAt(position, side), getBlockItemHandlersAt(position, side));
    }

    private Stream<IItemHandler> getEntityItemHandlersAt(final Vec3 position, final Direction side) {
        final AABB bounds = AABB.unitCubeFromLowerCorner(position.subtract(0.5, 0.5, 0.5));
        return entity.level.getEntities(entity, bounds).stream()
            .map(e -> e.getCapability(Capabilities.ITEM_HANDLER, side))
            .filter(LazyOptional::isPresent)
            .map(c -> c.orElseThrow(AssertionError::new));
    }

    private Stream<IItemHandler> getBlockItemHandlersAt(final Vec3 position, final Direction side) {
        final BlockPos pos = new BlockPos(position);
        final BlockEntity blockEntity = entity.level.getBlockEntity(pos);
        if (blockEntity == null) {
            return Stream.empty();
        }

        final LazyOptional<IItemHandler> capability = blockEntity.getCapability(Capabilities.ITEM_HANDLER, side);
        if (capability.isPresent()) {
            return Stream.of(capability.orElseThrow(AssertionError::new));
        }

        return Stream.empty();
    }

    private List<ItemEntity> getItemsInRange() {
        return entity.level.getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(1));
    }

    private int takeFromWorld(final int count) {
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemStackHandler inventory = robot.getInventory();

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

    private int takeFromInventories(final int count, final List<IItemHandler> handlers) {
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ItemStackHandler inventory = robot.getInventory();

        int remaining = count;
        for (final IItemHandler handler : handlers) {
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

    private int takeFromInventory(final int count, final IItemHandler handler, final int slot) {
        final ItemStackHandler inventory = robot.getInventory();
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
}

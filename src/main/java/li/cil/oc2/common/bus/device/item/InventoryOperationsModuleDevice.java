package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AABB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vec3;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.util.Optional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ContainerHelper;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InventoryOperationsModuleDevice extends IdentityProxy<ItemStack> implements RPCDevice, ItemDevice {
    private final Entity entity;
    private final Robot robot;
    private final ObjectDevice device;

    ///////////////////////////////////////////////////////////////////

    public InventoryOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity);
        this.entity = entity;
        this.robot = robot;
        this.device = new ObjectDevice(this, "inventory_operations");
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public List<String> getTypeNames() {
        return device.getTypeNames();
    }

    @Override
    public List<RPCMethod> getMethods() {
        return device.getMethods();
    }

    @Callback
    public void move(@Parameter("fromSlot") final int fromSlot,
                     @Parameter("intoSlot") final int intoSlot,
                     @Parameter("count") final int count) {
        if (count <= 0) {
            return;
        }

        final ContainerHelper inventory = robot.getInventory();

        // Do simulation run, validating slot indices and getting actual amount possible to move.
        ItemStack extracted = inventory.extractItem(fromSlot, count, true);
        ItemStack remaining = inventory.insertItem(intoSlot, extracted, true);

        // Do actual run, move as many as we know we can based on simulation.
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
    public int drop(@Parameter("count") final int count,
                    @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        ItemStack stack = robot.getInventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        for (final IItemHandler handler : getContainerHelpersInDirection(getAdjustedDirection(direction)).collect(Collectors.toList())) {
            stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);

            if (stack.isEmpty()) {
                break;
            }
        }

        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty() && dropped > 0) {
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
                        @Parameter("count") final int count,
                        @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return 0;
        }

        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        ItemStack stack = robot.getInventory().extractItem(selectedSlot, count, false);
        if (stack.isEmpty()) {
            return 0;
        }

        final int originalStackSize = stack.getCount();
        final Optional<IItemHandler> optional = getContainerHelpersInDirection(getAdjustedDirection(direction)).findFirst();
        if (optional.isPresent()) {
            stack = optional.get().insertItem(intoSlot, stack, false);
        }

        int dropped = originalStackSize - stack.getCount();
        if (!stack.isEmpty() && dropped > 0) {
            stack = robot.getInventory().insertItem(selectedSlot, stack, false);
        }

        if (!stack.isEmpty()) {
            dropped += stack.getCount();
            entity.spawnAtLocation(stack);
        }

        return dropped;
    }

    @Callback
    public int take(@Parameter("count") final int count,
                    @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return 0;
        }

        final List<IItemHandler> handlers = getContainerHelpersInDirection(getAdjustedDirection(direction)).collect(Collectors.toList());
        if (handlers.isEmpty()) {
            return takeFromWorld(count);
        } else {
            return takeFromInventories(count, handlers);
        }
    }

    @Callback
    public int takeFrom(@Parameter("fromSlot") final int fromSlot,
                        @Parameter("count") final int count,
                        @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return 0;
        }

        return getContainerHelpersInDirection(getAdjustedDirection(direction)).findFirst().map(handler ->
                takeFromInventory(count, handler, fromSlot)).orElse(0);
    }

    ///////////////////////////////////////////////////////////////////

    private Direction getAdjustedDirection(@Nullable Direction direction) {
        if (direction == null) {
            direction = Direction.SOUTH;
        }

        if (direction.getAxis().isHorizontal()) {
            final int horizontalIndex = entity.getDirection().get2DDataValue();
            for (int i = 0; i < horizontalIndex; i++) {
                direction = direction.getClockWise();
            }
        }

        return direction;
    }

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

    private Stream<IItemHandler> getContainerHelpersInDirection(final Direction direction) {
        final Vector3i directionVec = direction.getNormal();
        return getContainerHelpersAt(entity.position().add(Vec3.atCenterOf(directionVec)), direction.getOpposite());
    }

    private Stream<IItemHandler> getContainerHelpersAt(final Vec3 position, final Direction side) {
        return Stream.concat(getEntityItemHandlersAt(position, side), getBlockItemHandlersAt(position, side));
    }

    private Stream<IItemHandler> getEntityItemHandlersAt(final Vec3 position, final Direction side) {
        final AABB bounds = AABB.unitCubeFromLowerCorner(position.subtract(0.5, 0.5, 0.5));
        return entity.getCommandSenderWorld().getEntities(entity, bounds).stream()
                .map(e -> e.getCapability(Capabilities.ITEM_HANDLER, side))
                .filter(Optional::isPresent)
                .map(c -> c.orElseThrow(AssertionError::new));
    }

    private Stream<IItemHandler> getBlockItemHandlersAt(final Vec3 position, final Direction side) {
        final BlockPos pos = new BlockPos(position);
        final BlockEntity tileEntity = entity.getCommandSenderWorld().getBlockEntity(pos);
        if (tileEntity == null) {
            return Stream.empty();
        }

        final Optional<IItemHandler> capability = tileEntity.getCapability(Capabilities.ITEM_HANDLER, side);
        if (capability.isPresent()) {
            return Stream.of(capability.orElseThrow(AssertionError::new));
        }

        return Stream.empty();
    }

    private List<ItemEntity> getItemsInRange() {
        return entity.getCommandSenderWorld().getEntitiesOfClass(ItemEntity.class, entity.getBoundingBox().inflate(1));
    }

    private int takeFromWorld(final int count) {
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.
        final ContainerHelper inventory = robot.getInventory();

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
        final ContainerHelper inventory = robot.getInventory();

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

                // Do actual run, take as many as we know we can based on simulation.
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
        final ContainerHelper inventory = robot.getInventory();
        final int selectedSlot = robot.getSelectedSlot(); // Get once to avoid change due to threading.

        // Do simulation run, getting actual amount possible to take.
        ItemStack extracted = handler.extractItem(slot, count, true);
        ItemStack overflow = insertStartingAt(inventory, extracted, selectedSlot, true);

        int taken = extracted.getCount() - overflow.getCount();

        // Do actual run, take as many as we know we can based on simulation.
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

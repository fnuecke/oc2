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
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3i;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class InventoryAutomationRobotModuleDevice<T extends Entity & Robot> extends IdentityProxy<ItemStack> implements RPCDevice, ItemDevice {
    private final T robot;
    private final ObjectDevice device;

    ///////////////////////////////////////////////////////////////////

    public InventoryAutomationRobotModuleDevice(final ItemStack identity, final T robot) {
        super(identity);
        this.robot = robot;
        this.device = new ObjectDevice(this, "inventory_automation");
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

        final ItemStackHandler inventory = robot.getInventory();

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
            robot.entityDropItem(remaining);
        }
    }

    @Callback
    public void drop(@Parameter("count") final int count,
                     @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return;
        }

        ItemStack stack = robot.getInventory().extractItem(robot.getSelectedSlot(), count, false);
        if (stack.isEmpty()) {
            return;
        }

        final int originalStackSize = stack.getCount();
        for (final IItemHandler handler : getItemStackHandlersInDirection(getAdjustedDirection(direction)).collect(Collectors.toList())) {
            stack = ItemHandlerHelper.insertItemStacked(handler, stack, false);
        }

        final boolean droppedSome = stack.getCount() != originalStackSize;
        if (!stack.isEmpty() && droppedSome) {
            stack = robot.getInventory().insertItem(robot.getSelectedSlot(), stack, false);
        }

        if (!stack.isEmpty()) {
            robot.entityDropItem(stack);
        }
    }

    @Callback
    public void dropInto(@Parameter("intoSlot") final int intoSlot,
                  @Parameter("count") final int count,
                  @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return;
        }

        ItemStack stack = robot.getInventory().extractItem(robot.getSelectedSlot(), count, false);
        if (stack.isEmpty()) {
            return;
        }

        final int originalStackSize = stack.getCount();
        final Optional<IItemHandler> optional = getItemStackHandlersInDirection(getAdjustedDirection(direction)).findFirst();
        if (optional.isPresent()) {
            final IItemHandler handler = optional.get();

            stack = handler.insertItem(intoSlot, stack, false);
        }

        final boolean droppedSome = stack.getCount() != originalStackSize;
        if (!stack.isEmpty() && droppedSome) {
            stack = robot.getInventory().insertItem(robot.getSelectedSlot(), stack, false);
        }

        if (!stack.isEmpty()) {
            robot.entityDropItem(stack);
        }
    }

    @Callback
    public void take(@Parameter("count") int count,
                     @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return;
        }

        final ItemStackHandler inventory = robot.getInventory();
        final int selectedSlot = robot.getSelectedSlot();

        final List<IItemHandler> handlers = getItemStackHandlersInDirection(getAdjustedDirection(direction)).collect(Collectors.toList());
        for (final IItemHandler handler : handlers) {
            for (int fromSlot = 0; fromSlot < handler.getSlots(); fromSlot++) {
                // Do simulation run, getting actual amount possible to take.
                ItemStack extracted = handler.extractItem(fromSlot, count, true);
                ItemStack remaining = insertStartingAt(inventory, extracted, selectedSlot, true);

                count -= extracted.getCount() - remaining.getCount();

                // Do actual run, take as many as we know we can based on simulation.
                extracted = handler.extractItem(fromSlot, extracted.getCount() - remaining.getCount(), false);
                remaining = insertStartingAt(inventory, extracted, selectedSlot, false);

                // But don't trust simulation; if something is remaining after actual run, try to put it back.
                remaining = handler.insertItem(fromSlot, remaining, false);

                // And if putting it back fails, just drop it. Avoid destroying items.
                if (!remaining.isEmpty()) {
                    robot.entityDropItem(remaining);
                }
            }

            if (count <= 0) {
                break;
            }
        }
    }

    @Callback
    public void takeFrom(@Parameter("fromSlot") final int fromSlot,
                         @Parameter("count") final int count,
                         @Parameter("direction") @Nullable final Direction direction) {
        if (count <= 0) {
            return;
        }

        final ItemStackHandler inventory = robot.getInventory();
        final int selectedSlot = robot.getSelectedSlot();

        final Optional<IItemHandler> optional = getItemStackHandlersInDirection(getAdjustedDirection(direction)).findFirst();
        if (optional.isPresent()) {
            final IItemHandler handler = optional.get();

            // Do simulation run, getting actual amount possible to take.
            ItemStack extracted = handler.extractItem(fromSlot, count, true);
            ItemStack remaining = insertStartingAt(inventory, extracted, selectedSlot, true);

            // Do actual run, take as many as we know we can based on simulation.
            extracted = handler.extractItem(fromSlot, extracted.getCount() - remaining.getCount(), false);
            remaining = insertStartingAt(inventory, extracted, selectedSlot, false);

            // But don't trust simulation; if something is remaining after actual run, try to put it back.
            remaining = handler.insertItem(fromSlot, remaining, false);

            // And if putting it back fails, just drop it. Avoid destroying items.
            if (!remaining.isEmpty()) {
                robot.entityDropItem(remaining);
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    private Direction getAdjustedDirection(@Nullable Direction direction) {
        if (direction == null) {
            direction = Direction.SOUTH;
        }

        if (direction.getAxis().isHorizontal()) {
            final int horizontalIndex = robot.getHorizontalFacing().getHorizontalIndex();
            for (int i = 0; i < horizontalIndex; i++) {
                direction = direction.rotateY();
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

    private Stream<IItemHandler> getItemStackHandlersInDirection(final Direction direction) {
        final Vector3i directionVec = direction.getDirectionVec();
        return getItemStackHandlersAt(robot.getPositionVec().add(Vector3d.copy(directionVec)), direction.getOpposite());
    }

    private Stream<IItemHandler> getItemStackHandlersAt(final Vector3d position, final Direction side) {
        return Stream.concat(getEntityItemHandlersAt(position, side), getBlockItemHandlersAt(position, side));
    }

    private Stream<IItemHandler> getEntityItemHandlersAt(final Vector3d position, final Direction side) {
        final AxisAlignedBB bounds = AxisAlignedBB.fromVector(position.subtract(0.5, 0.5, 0.5));
        return robot.getEntityWorld().getEntitiesWithinAABBExcludingEntity(robot, bounds).stream()
                .map(e -> e.getCapability(Capabilities.ITEM_HANDLER, side))
                .filter(LazyOptional::isPresent)
                .map(c -> c.orElseThrow(AssertionError::new));
    }

    private Stream<IItemHandler> getBlockItemHandlersAt(final Vector3d position, final Direction side) {
        // TODO may want use blockpos iterator to get blocks we currently sit in, e.g. if during move, and check for all

        final BlockPos pos = new BlockPos(position);
        final TileEntity tileEntity = robot.getEntityWorld().getTileEntity(pos);
        if (tileEntity == null) {
            return Stream.empty();
        }

        final LazyOptional<IItemHandler> capability = tileEntity.getCapability(Capabilities.ITEM_HANDLER, side);
        if (capability.isPresent()) {
            return Stream.of(capability.orElseThrow(AssertionError::new));
        }

        return Stream.empty();
    }
}

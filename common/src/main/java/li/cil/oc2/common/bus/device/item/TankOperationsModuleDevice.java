/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.api.capabilities.Robot;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.bus.device.util.FluidHandlerDeviceUtils;
import li.cil.oc2.common.bus.device.util.FluidHandlerProtocol;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidHandlerUtils;
import li.cil.oc2.common.fluid.FluidStack;
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

@RPCDeviceDescription(typeNames = {"tank_operations"}, description = """
    Provided by the [tank operations module](../item/tank_operations_module.md) to robots.

    The side parameter in the following methods represents a direction from the perspective of the robot. Valid values are: `front`, `up` and `down`.

    Allows operating the robot's tank, as well as inspecting and operating block and entity tanks. If neither external tank type is present, space permitting, fluids are poured or drained, one bucket at a time.

    Sides without a container report no tank, which allows predicting world interaction, e.g. to ensure things are never dumped into the world.""")
@IODeviceDescription(name = "TNKOPS", description = """
    Sides are numbered: `0` front, `1` up and `2` down. Fluid ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup. Amounts are millibuckets, four bytes, low byte first.""")
public final class TankOperationsModuleDevice extends AbstractItemDevice {
    private static final int FILL_CODE = 1;
    private static final int DRAIN_CODE = 2;
    private static final int MOVE_INTO_CODE = 3;
    private static final int MOVE_FROM_CODE = 4;
    private static final int GET_FLUID_TANK_COUNT_CODE = 5;
    private static final int GET_FLUID_TANKS_CODE = 6;
    private static final int GET_FLUID_TANK_CAPACITY_CODE = 7;

    private static final String FILL_DESCRIPTION = "Tries to move fluid from the robot's tank into a tank in the specified direction.";
    private static final String DRAIN_DESCRIPTION = "Tries to move fluid from a tank in the specified direction into the robot's tank.";
    private static final String MOVE_INTO_DESCRIPTION = "Tries to move fluid from the robot's tank into the container item, such as a bucket, in the selected slot.";
    private static final String MOVE_FROM_DESCRIPTION = "Tries to move fluid from the container item in the selected slot into the robot's tank.";
    private static final String TANK_TO_LOOK_AT = "the number of the tank to inspect.";
    private static final String SIDE_DESCRIPTION = "`front`, `up` or `down`. Optional, defaults to `front`.";

    private final Entity entity;
    private final Robot robot;

    // --------------------------------------------------------------------- //

    public TankOperationsModuleDevice(final ItemStack identity, final Entity entity, final Robot robot) {
        super(identity);
        this.entity = entity;
        this.robot = robot;
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many tanks the fluid container on the specified side has.",
        returnValueDescription = "the number of tanks, or `0` if there is no fluid container on that side.")
    public int getFluidTankCount() {
        return getFluidTankCount(null);
    }

    @Callback(description = "Gets how many tanks the fluid container on the specified side has.",
        returnValueDescription = "the number of tanks, or `0` if there is no fluid container on that side.")
    public int getFluidTankCount(@Parameter(value = "side", description = SIDE_DESCRIPTION, optional = true) @Nullable final RobotOperationSide side) {
        final List<FluidHandler> handlers = getFluidHandlers(side);
        return handlers.isEmpty() ? 0 : handlers.getFirst().getTanks();
    }

    @Callback(description = "Gets what is in the specified tank of the container on the specified side.",
        returnValueDescription = "a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.")
    public FluidStack getFluidInTank(@Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank) {
        return getFluidInTank(tank, null);
    }

    @Callback(description = "Gets what is in the specified tank of the container on the specified side.",
        returnValueDescription = "a table with the fluid `id` and the `amount` in millibuckets. Returns nothing for an empty tank.")
    public FluidStack getFluidInTank(@Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank,
                                     @Parameter(value = "side", description = SIDE_DESCRIPTION, optional = true) @Nullable final RobotOperationSide side) {
        final FluidHandler handler = requireFluidHandlers(side).getFirst();
        return handler.getFluidInTank(FluidHandlerDeviceUtils.requireValidTank(handler, tank));
    }

    @Callback(description = "Gets how much the specified tank of the container on the specified side can hold.",
        returnValueDescription = "the capacity in millibuckets.")
    public int getFluidTankCapacity(@Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank) {
        return getFluidTankCapacity(tank, null);
    }

    @Callback(description = "Gets how much the specified tank of the container on the specified side can hold.",
        returnValueDescription = "the capacity in millibuckets.")
    public int getFluidTankCapacity(@Parameter(value = "tank", description = TANK_TO_LOOK_AT) final int tank,
                                    @Parameter(value = "side", description = SIDE_DESCRIPTION, optional = true) @Nullable final RobotOperationSide side) {
        final FluidHandler handler = requireFluidHandlers(side).getFirst();
        return handler.getTankCapacity(FluidHandlerDeviceUtils.requireValidTank(handler, tank));
    }

    @Callback(energy = 1, description = FILL_DESCRIPTION, returnValueDescription = "the amount moved in millibuckets.")
    public int fill(@Parameter(value = "amount", description = "the most millibuckets to move.") final int amount) {
        return fill(amount, null);
    }

    @Callback(energy = 1, description = FILL_DESCRIPTION, returnValueDescription = "the amount moved in millibuckets.")
    public int fill(@Parameter(value = "amount", description = "the most millibuckets to move.") final int amount,
                    @Parameter(value = "side", description = SIDE_DESCRIPTION, optional = true) @Nullable final RobotOperationSide side) {
        return FluidHandlerUtils.transferFirst(List.of(tank()), requireFluidHandlersOrWorld(side), amount);
    }

    @Callback(energy = 1, description = DRAIN_DESCRIPTION, returnValueDescription = "the amount moved in millibuckets.")
    public int drain(@Parameter(value = "amount", description = "the most millibuckets to move.") final int amount) {
        return drain(amount, null);
    }

    @Callback(energy = 1, description = DRAIN_DESCRIPTION, returnValueDescription = "the amount moved in millibuckets.")
    public int drain(@Parameter(value = "amount", description = "the most millibuckets to move.") final int amount,
                     @Parameter(value = "side", description = SIDE_DESCRIPTION, optional = true) @Nullable final RobotOperationSide side) {
        return FluidHandlerUtils.transferFirst(requireFluidHandlersOrWorld(side), List.of(tank()), amount);
    }

    @Callback(energy = 1, description = MOVE_INTO_DESCRIPTION, returnValueDescription = "the amount moved in millibuckets.")
    public int moveInto(@Parameter(value = "amount", description = "the most millibuckets to move.") final int amount) {
        final FluidHandler item = FluidHandlerDeviceUtils.requireContainerItem(inventory(), robot.getSelectedSlot());
        return FluidHandlerUtils.transfer(tank(), amount, (stack, simulate) -> FluidHandlerUtils.insertFluidStack(item, stack, simulate));
    }

    @Callback(energy = 1, description = MOVE_FROM_DESCRIPTION, returnValueDescription = "the amount moved in millibuckets.")
    public int moveFrom(@Parameter(value = "amount", description = "the most millibuckets to move.") final int amount) {
        final FluidHandler item = FluidHandlerDeviceUtils.requireContainerItem(inventory(), robot.getSelectedSlot());
        return FluidHandlerUtils.transfer(item, amount, (stack, simulate) -> FluidHandlerUtils.insertFluidStack(tank(), stack, simulate));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_FLUID_TANK_COUNT_CODE,
        description = "Reads how many tanks the container on that side has.",
        argumentsDescription = "one byte, the side.",
        resultsDescription = "one byte, the tank count, at most 255. A side without a fluid container reads as 0.")
    public void getFluidTankCount(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(getFluidTankCount(RobotOperationSide.byIndex(arguments.readU8())), 0xFF));
    }

    @IOCallback(value = GET_FLUID_TANKS_CODE,
        description = "Reads a run of tanks of the container on that side.",
        argumentsDescription = "three bytes, the side, the tank to start at and how many tanks to read, from 1 to 42.",
        resultsDescription = "six bytes per tank: the fluid as two bytes and the amount as four bytes.")
    public void getFluidTanks(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(requireFluidHandlers(RobotOperationSide.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(value = GET_FLUID_TANK_CAPACITY_CODE,
        description = "Reads how much a tank of the container on that side can hold.",
        argumentsDescription = "two bytes, the side and the tank.",
        resultsDescription = "four bytes, the capacity.")
    public void getFluidTankCapacity(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(requireFluidHandlers(RobotOperationSide.byIndex(arguments.readU8())).getFirst(), arguments, results);
    }

    @IOCallback(energy = 1, value = FILL_CODE,
        description = FILL_DESCRIPTION,
        argumentsDescription = "five bytes: four bytes for the most millibuckets to move, and the side.",
        resultsDescription = "four bytes, the amount moved.")
    public void fill(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int amount = (int) Math.min(arguments.readU32(), Integer.MAX_VALUE);
        results.writeU32(fill(amount, RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(energy = 1, value = DRAIN_CODE,
        description = DRAIN_DESCRIPTION,
        argumentsDescription = "five bytes: four bytes for the most millibuckets to move, and the side.",
        resultsDescription = "four bytes, the amount moved.")
    public void drain(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int amount = (int) Math.min(arguments.readU32(), Integer.MAX_VALUE);
        results.writeU32(drain(amount, RobotOperationSide.byIndex(arguments.readU8())));
    }

    @IOCallback(energy = 1, value = MOVE_INTO_CODE,
        description = MOVE_INTO_DESCRIPTION,
        argumentsDescription = "four bytes, the most millibuckets to move.",
        resultsDescription = "four bytes, the amount moved.")
    public void moveInto(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU32(moveInto((int) Math.min(arguments.readU32(), Integer.MAX_VALUE)));
    }

    @IOCallback(energy = 1, value = MOVE_FROM_CODE,
        description = MOVE_FROM_DESCRIPTION,
        argumentsDescription = "four bytes, the most millibuckets to move.",
        resultsDescription = "four bytes, the amount moved.")
    public void moveFrom(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU32(moveFrom((int) Math.min(arguments.readU32(), Integer.MAX_VALUE)));
    }

    // --------------------------------------------------------------------- //

    private List<FluidHandler> getFluidHandlers(@Nullable final RobotOperationSide side) {
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return Capabilities.getAll((ServerLevel) entity.level(), entity.blockPosition().relative(direction),
            Capabilities.FLUID_HANDLER, direction.getOpposite(), entity);
    }

    private List<FluidHandler> requireFluidHandlers(@Nullable final RobotOperationSide side) {
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        return FluidHandlerDeviceUtils.requireFluidHandlers((ServerLevel) entity.level(), entity.blockPosition().relative(direction),
            direction.getOpposite(), entity, side != null ? side : RobotOperationSide.FRONT);
    }

    private List<FluidHandler> requireFluidHandlersOrWorld(@Nullable final RobotOperationSide side) {
        final Direction direction = RobotOperationSide.toGlobal(entity, side);
        final ServerLevel level = (ServerLevel) entity.level();
        return FluidHandlerDeviceUtils.requireFluidHandlersOrWorld(level, entity.blockPosition().relative(direction), direction.getOpposite(),
            entity, FakePlayerUtils.getFakePlayer(level, entity), side != null ? side : RobotOperationSide.FRONT);
    }

    private ItemHandler inventory() {
        return Objects.requireNonNull(Capabilities.get(entity, Capabilities.ITEM_HANDLER, null));
    }

    private FluidHandler tank() {
        return Objects.requireNonNull(Capabilities.get(entity, Capabilities.FLUID_HANDLER, null));
    }
}

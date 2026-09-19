/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fixture;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.api.util.RobotOperationSide;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.energy.EnergyStorage;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.VirtualMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;

import java.util.List;

import static li.cil.oc2.gametest.util.TestSupport.fakePlayer;
import static li.cil.oc2.gametest.util.TestSupport.useOn;

public final class RobotFixture implements MachineFixture {
    private final GameTestHelper helper;
    private final Robot robot;

    // --------------------------------------------------------------------- //

    public static RobotFixture place(final GameTestHelper helper, final BlockPos pos) {
        return place(helper, fakePlayer(helper), pos);
    }

    public static RobotFixture place(final GameTestHelper helper, final Player player, final BlockPos pos) {
        useOn(helper, player, new ItemStack(Items.ROBOT.get()), pos, Direction.UP);

        final AABB bounds = new AABB(helper.absolutePos(pos)).inflate(2);
        final List<Robot> robots = helper.getLevel().getEntitiesOfClass(Robot.class, bounds);
        if (robots.size() != 1) {
            throw new GameTestAssertException("expected exactly one robot near " + pos + ", found " + robots.size());
        }

        return new RobotFixture(helper, robots.getFirst());
    }

    // --------------------------------------------------------------------- //

    public static RobotFixture of(final GameTestHelper helper, final Robot robot) {
        return new RobotFixture(helper, robot);
    }

    // --------------------------------------------------------------------- //

    @Override
    public VirtualMachine virtualMachine() {
        return robot.getVirtualMachine();
    }

    @Override
    public Terminal terminal() {
        return robot.getTerminal();
    }

    @Override
    public ItemHandler handler(final DeviceType type) {
        return robot.getItemStackHandlers().getItemHandler(type)
            .orElseThrow(() -> new GameTestAssertException("no item handler for " + type));
    }

    @Override
    public String describe() {
        return "runState=" + virtualMachine().getRunState()
            + ", bootError=" + virtualMachine().getBootError()
            + ", error=" + virtualMachine().getError();
    }

    public Robot entity() {
        return robot;
    }

    public BlockPos blockPos() {
        return robot.blockPosition();
    }

    public BlockPos frontPos() {
        return robot.blockPosition().relative(RobotOperationSide.toGlobal(robot, RobotOperationSide.FRONT));
    }

    public void start() {
        robot.start();
    }

    public void stop() {
        robot.stop();
    }

    public ItemHandler inventory() {
        return robot.getInventory();
    }

    public ItemStack selected() {
        return robot.getInventory().getStackInSlot(robot.getSelectedSlot());
    }

    public RobotFixture give(final ItemStack stack) {
        robot.getInventory().insertItem(robot.getSelectedSlot(), stack, false);
        return this;
    }

    @Override
    public RobotFixture install(final DeviceType type, final ItemStack stack) {
        installInto(type, stack);
        return this;
    }

    public boolean has(final Item item) {
        final ItemHandler inventory = robot.getInventory();
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            if (inventory.getStackInSlot(slot).is(item)) {
                return true;
            }
        }

        return false;
    }

    public BlockPos putBlockInFront(final Block block) {
        final BlockPos target = frontPos();
        helper.getLevel().setBlockAndUpdate(target, block.defaultBlockState());
        return target;
    }

    public RobotFixture charge() {
        final EnergyStorage storage = robot.getCapability(Capabilities.ENERGY_STORAGE, null);
        if (storage != null) {
            storage.receiveEnergy(storage.getMaxEnergyStored(), false);
        }
        return this;
    }

    // --------------------------------------------------------------------- //

    private RobotFixture(final GameTestHelper helper, final Robot robot) {
        this.helper = helper;
        this.robot = robot;
    }
}

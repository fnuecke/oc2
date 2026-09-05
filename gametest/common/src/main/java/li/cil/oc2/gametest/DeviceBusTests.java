/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.FlashDriveDevice;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import static li.cil.oc2.gametest.TestSupport.*;

public final class DeviceBusTests {
    public static void busTracksNeighborLifecycle(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> base[0] = computer.deviceCount())
            .thenExecute(() -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = computer.deviceCount();
                if (withNeighbor <= base[0]) {
                    throw new GameTestAssertException(
                        "attaching a redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ")");
                }
            })
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenExecuteAfter(60, () -> {
                final int after = computer.deviceCount();
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "device count did not return to baseline after removing the neighbour: alone="
                            + base[0] + ", after removal=" + after
                            + " (capability invalidation is not propagating)");
                }
            })
            .thenSucceed();
    }

    public static void busRediscoversReplacedNeighbor(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] attached = new int[1];
        helper.startSequence()
            .thenExecuteAfter(40, () -> placeDevice(helper))
            .thenExecuteAfter(60, () -> attached[0] = computer.deviceCount())
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenExecuteAfter(60, () -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                final int rediscovered = computer.deviceCount();
                if (rediscovered < attached[0]) {
                    throw new GameTestAssertException(
                        "neighbour not rediscovered after being replaced: first attach="
                            + attached[0] + ", after replace=" + rediscovered);
                }
            })
            .thenSucceed();
    }

    public static void busNoticesNeighborCapabilityInvalidatedWithoutBlockUpdate(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);

        final int[] base = new int[1];
        final int[] attached = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> base[0] = computer.deviceCount())
            .thenExecute(() -> helper.setBlock(DEVICE_POS, Blocks.CHEST))
            .thenWaitUntil(() -> {
                attached[0] = computer.deviceCount();
                if (attached[0] <= base[0]) {
                    throw new GameTestAssertException(
                        "chest did not attach in the first place (alone=" + base[0]
                            + ", attached=" + attached[0] + ")");
                }
            })
            .thenExecute(() -> helper.getLevel().setBlock(helper.absolutePos(DEVICE_POS),
                Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS))
            .thenWaitUntil(() -> {
                final int after = computer.deviceCount();
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "bus did not notice its neighbour's capability going away without a block update: alone="
                            + base[0] + ", attached=" + attached[0] + ", after=" + after);
                }
            })
            .thenSucceed();
    }

    public static void busReachesAcrossChunkBoundary(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);

        final int z = CABLE_POS.getZ();
        final BlockPos lastCable = new BlockPos(MAX_X - 1, WORK_Y, z);
        final BlockPos farDevice = new BlockPos(MAX_X, WORK_Y, z);
        BusCables.placeCableRun(helper, player, CABLE_POS, lastCable);
        BusCables.placeInterface(helper, player, CABLE_POS, Direction.WEST);
        BusCables.placeInterface(helper, player, lastCable, Direction.EAST);

        if (helper.absolutePos(CABLE_POS).getX() >> 4 == helper.absolutePos(lastCable).getX() >> 4) {
            throw new GameTestAssertException("the cable run does not cross a chunk boundary");
        }

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(80, () -> base[0] = computer.deviceCount())
            .thenExecute(() -> place(helper, fakePlayer(helper),
                new ItemStack(Items.REDSTONE_INTERFACE.get()), farDevice))
            .thenExecuteAfter(80, () -> {
                final int reached = computer.deviceCount();
                if (reached <= base[0]) {
                    throw new GameTestAssertException(
                        "device at the far end of a cross-chunk cable run was not found (run="
                            + base[0] + ", with device=" + reached + ")");
                }
            })
            .thenSucceed();
    }

    public static void blockVmDeviceIsReachableFromItsMountingFaceOnly(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);

        // The drive sits on top of the first cable, so its mounting face is the one that cable's
        // interface touches. The second cable is diagonal to the first, i.e. a separate bus.
        final BlockPos drivePos = CABLE_POS.above();
        final BlockPos otherCable = drivePos.east();

        final ComputerFixture owner = ComputerFixture.place(helper, player, COMPUTER_POS);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.UP);
        useOn(helper, player, new ItemStack(Items.FLASH_DRIVE.get()), CABLE_POS, Direction.UP);
        if (!(helper.getBlockEntity(drivePos) instanceof FlashDriveBlockEntity)) {
            throw new GameTestAssertException("no flash drive at " + drivePos);
        }

        BusCables.placeCableWithInterfaces(helper, player, otherCable, Direction.WEST, Direction.EAST);
        final ComputerFixture bystander = ComputerFixture.place(helper, player, otherCable.east());

        helper.startSequence()
            .thenExecuteAfter(80, () -> {
                if (countFlashDrives(owner) == 0) {
                    throw new GameTestAssertException(
                        "the bus touching the drive's mounting face does not see it: " + owner.describe());
                }
                if (countFlashDrives(bystander) != 0) {
                    throw new GameTestAssertException(
                        "a bus touching a side face sees the drive, so two bus controllers mount the same "
                            + "device and both open its blob: " + bystander.describe());
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ComputerFixture placeComputerAndCable(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.EAST);
        return computer;
    }

    private static long countFlashDrives(final ComputerFixture computer) {
        return computer.devices().stream().filter(FlashDriveDevice.class::isInstance).count();
    }

    private static void placeDevice(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTests() {
    }
}

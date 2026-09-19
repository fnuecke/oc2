/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import li.cil.oc2.common.bus.device.vm.block.FlashDriveDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.util.BusCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.lang.ref.WeakReference;

import static li.cil.oc2.gametest.util.TestSupport.*;

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

        final int[] baseline = new int[1];
        final int[] attached = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> baseline[0] = computer.deviceCount())
            .thenExecute(() -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                attached[0] = computer.deviceCount();
                if (attached[0] <= baseline[0]) {
                    throw new GameTestAssertException("precondition: the device never attached, count stayed at "
                        + baseline[0]);
                }
            })
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenExecuteAfter(60, () -> {
                if (computer.deviceCount() != baseline[0]) {
                    throw new GameTestAssertException("breaking the neighbour left its devices on the bus: "
                        + computer.deviceCount() + ", expected " + baseline[0]);
                }
            })
            .thenExecute(() -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                final int rediscovered = computer.deviceCount();
                if (rediscovered != attached[0]) {
                    throw new GameTestAssertException(
                        "neighbour not rediscovered after being replaced: first attach="
                            + attached[0] + ", after replace=" + rediscovered);
                }
            })
            .thenSucceed();
    }

    public static void busDropsNeighborWithoutBlockUpdate(final GameTestHelper helper) {
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
            .thenExecute(() -> collectGarbage(helper))
            .thenExecute(() -> helper.getLevel().setBlock(helper.absolutePos(DEVICE_POS),
                Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS))
            .thenWaitUntil(() -> {
                final int after = computer.deviceCount();
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "bus kept the device after its capability went away: alone="
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

    public static void blockDeviceMountsFromOfferingFaceOnly(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);

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
                        "drive is visible from a side face: " + bystander.describe());
                }
            })
            .thenSucceed();
    }

    public static void noteBlockJoinsAndLeavesTheBus(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerAndCable(helper);
        helper.setBlock(DEVICE_POS, Blocks.NOTE_BLOCK);

        helper.startSequence()
            .thenWaitUntil(() -> {
                if (countNoteBlocks(computer) != 1) {
                    throw new GameTestAssertException("the note block is not on the bus: " + computer.describe());
                }
            })
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenWaitUntil(() -> {
                if (countNoteBlocks(computer) != 0) {
                    throw new GameTestAssertException("the removed note block is still on the bus: " + computer.describe());
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

    private static long countNoteBlocks(final ComputerFixture computer) {
        return computer.devices().stream()
            .filter(device -> device instanceof final RPCDevice rpcDevice && rpcDevice.getTypeNames().contains("note_block"))
            .count();
    }

    private static void placeDevice(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);
    }

    private static void collectGarbage(final GameTestHelper helper) {
        final WeakReference<Object> canary = new WeakReference<>(new Object());
        for (int attempt = 0; attempt < 10 && canary.get() != null; attempt++) {
            System.gc();
        }
        assertTrue(helper, "gc didn't want to run, but we need it to for this test", canary.get() == null);
    }

    // --------------------------------------------------------------------- //

    private DeviceBusTests() {
    }
}

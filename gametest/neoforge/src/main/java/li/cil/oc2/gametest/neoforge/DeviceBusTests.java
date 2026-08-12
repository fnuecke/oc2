/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.block.ComputerBlock;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class DeviceBusTests {
    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void busTracksNeighborLifecycle(final GameTestHelper helper) {
        placeComputerAndCable(helper);

        final int[] base = new int[1];
        helper.startSequence()
            .thenExecuteAfter(60, () -> base[0] = deviceCount(helper))
            .thenExecute(() -> helper.setBlock(DEVICE_POS, Blocks.REDSTONE_INTERFACE.get()))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = deviceCount(helper);
                if (withNeighbor <= base[0]) {
                    throw new GameTestAssertException(
                        "attaching a redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ")");
                }
            })
            .thenExecute(() -> helper.setBlock(DEVICE_POS, net.minecraft.world.level.block.Blocks.AIR))
            .thenExecuteAfter(60, () -> {
                final int after = deviceCount(helper);
                if (after != base[0]) {
                    throw new GameTestAssertException(
                        "device count did not return to baseline after removing the neighbour: alone="
                            + base[0] + ", after removal=" + after
                            + " (capability invalidation is not propagating)");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 800)
    public static void busRediscoversReplacedNeighbor(final GameTestHelper helper) {
        placeComputerAndCable(helper);

        final int[] attached = new int[1];
        helper.startSequence()
            .thenExecuteAfter(40, () -> helper.setBlock(DEVICE_POS, Blocks.REDSTONE_INTERFACE.get()))
            .thenExecuteAfter(60, () -> attached[0] = deviceCount(helper))
            .thenExecute(() -> helper.setBlock(DEVICE_POS, net.minecraft.world.level.block.Blocks.AIR))
            .thenExecuteAfter(60, () -> helper.setBlock(DEVICE_POS, Blocks.REDSTONE_INTERFACE.get()))
            .thenExecuteAfter(60, () -> {
                final int rediscovered = deviceCount(helper);
                if (rediscovered < attached[0]) {
                    throw new GameTestAssertException(
                        "neighbour not rediscovered after being replaced: first attach="
                            + attached[0] + ", after replace=" + rediscovered);
                }
            })
            .thenSucceed();
    }

    ///////////////////////////////////////////////////////////////////

    private static void placeComputerAndCable(final GameTestHelper helper) {
        helper.setBlock(COMPUTER_POS, Blocks.COMPUTER.get().defaultBlockState()
            .setValue(ComputerBlock.FACING, Direction.NORTH));
        helper.setBlock(CABLE_POS, Blocks.BUS_CABLE.get().defaultBlockState()
            .setValue(BusCableBlock.HAS_CABLE, true)
            .setValue(BusCableBlock.CONNECTION_WEST, BusCableBlock.ConnectionType.CABLE)
            .setValue(BusCableBlock.CONNECTION_EAST, BusCableBlock.ConnectionType.INTERFACE));
    }

    private static int deviceCount(final GameTestHelper helper) {
        final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
        return ((AbstractVirtualMachine) computer.getVirtualMachine()).busController.getDevices().size();
    }

    private DeviceBusTests() {
    }
}

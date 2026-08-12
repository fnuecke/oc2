/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
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
            .thenExecute(() -> placeDevice(helper))
            .thenExecuteAfter(60, () -> {
                final int withNeighbor = deviceCount(helper);
                if (withNeighbor <= base[0]) {
                    throw new GameTestAssertException(
                        "attaching a redstone interface added no device (alone=" + base[0]
                            + ", attached=" + withNeighbor + ")");
                }
            })
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
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
            .thenExecuteAfter(40, () -> placeDevice(helper))
            .thenExecuteAfter(60, () -> attached[0] = deviceCount(helper))
            .thenExecute(() -> breakBlock(helper, DEVICE_POS))
            .thenExecuteAfter(60, () -> placeDevice(helper))
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
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);
        place(helper, player, new ItemStack(Items.BUS_CABLE.get()), CABLE_POS);
        useOn(helper, player, new ItemStack(Items.BUS_INTERFACE.get()), CABLE_POS, Direction.WEST);
        useOn(helper, player, new ItemStack(Items.BUS_INTERFACE.get()), CABLE_POS, Direction.EAST);
    }

    private static void placeDevice(final GameTestHelper helper) {
        place(helper, fakePlayer(helper), new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);
    }

    private static int deviceCount(final GameTestHelper helper) {
        final ComputerBlockEntity computer = helper.getBlockEntity(COMPUTER_POS);
        return ((AbstractVirtualMachine) computer.getVirtualMachine()).busController.getDevices().size();
    }

    private DeviceBusTests() {
    }
}

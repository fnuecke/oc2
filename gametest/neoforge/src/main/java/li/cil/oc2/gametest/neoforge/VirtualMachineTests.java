/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class VirtualMachineTests {
    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void computerWithHardwareRunsAndStops(final GameTestHelper helper) {
        placeMachine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(helper))
            .thenExecuteAfter(20, () -> computer(helper).start())
            .thenExecuteAfter(300, () -> {
                final var vm = computer(helper).getVirtualMachine();
                if (vm.getRunState() != VMRunState.RUNNING) {
                    throw new GameTestAssertException("computer did not reach RUNNING, was "
                        + vm.getRunState() + ", bootError=" + vm.getBootError());
                }
            })
            .thenExecute(() -> computer(helper).stop())
            .thenExecuteAfter(60, () -> {
                final var vm = computer(helper).getVirtualMachine();
                if (vm.getRunState() != VMRunState.STOPPED) {
                    throw new GameTestAssertException("computer did not stop, was " + vm.getRunState());
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void runningComputerConsumesEnergy(final GameTestHelper helper) {
        placeMachine(helper);

        final long[] charged = new long[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(helper))
            .thenExecuteAfter(20, () -> computer(helper).start())
            .thenExecuteAfter(200, () -> {
                charged[0] = energy(helper);
                if (charged[0] <= 0) {
                    throw new GameTestAssertException("computer never charged from the creative source");
                }
                breakBlock(helper, POWER_POS);
            })
            .thenExecuteAfter(120, () -> {
                final long drained = energy(helper);
                if (drained >= charged[0]) {
                    throw new GameTestAssertException("energy did not drain once the source was removed: "
                        + charged[0] + " -> " + drained);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void runningStateSurvivesNbtRoundTrip(final GameTestHelper helper) {
        placeMachine(helper);

        final CompoundTag[] saved = new CompoundTag[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(helper))
            .thenExecuteAfter(20, () -> computer(helper).start())
            .thenExecuteAfter(300, () -> {
                final var vm = computer(helper).getVirtualMachine();
                if (vm.getRunState() != VMRunState.RUNNING) {
                    throw new GameTestAssertException("precondition: computer is not running, was "
                        + vm.getRunState() + ", bootError=" + vm.getBootError());
                }

                saved[0] = computer(helper).saveWithFullMetadata(helper.getLevel().registryAccess());
                if (!saved[0].contains("state")) {
                    throw new GameTestAssertException("saved tag carries no VM state");
                }
            })
            .thenExecute(() -> computer(helper)
                .loadWithComponents(saved[0], helper.getLevel().registryAccess()))
            .thenExecuteAfter(200, () -> {
                final var vm = computer(helper).getVirtualMachine();
                if (vm.getRunState() != VMRunState.RUNNING) {
                    throw new GameTestAssertException("computer did not survive the round trip, was "
                        + vm.getRunState() + ", bootError=" + vm.getBootError());
                }
                if (vm.getError() != null) {
                    throw new GameTestAssertException("VM reported an error after reload: " + vm.getError());
                }
            })
            .thenSucceed();
    }

    ///////////////////////////////////////////////////////////////////

    private static void placeMachine(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), POWER_POS);
    }

    private static void installHardware(final GameTestHelper helper) {
        install(helper, DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()));
        install(helper, DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()));
        install(helper, DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_CUSTOM.get()));
    }

    private static void install(final GameTestHelper helper, final DeviceType type, final ItemStack stack) {
        final ItemStack leftover = computer(helper).getItemStackHandlers()
            .getItemHandler(type)
            .orElseThrow(() -> new GameTestAssertException("no item handler for " + type))
            .insertItem(0, stack, false);
        if (!leftover.isEmpty()) {
            throw new GameTestAssertException("could not install " + stack);
        }
    }

    private static long energy(final GameTestHelper helper) {
        final var storage = Capabilities.get(computer(helper), Capabilities.ENERGY_STORAGE, null);
        if (storage == null) {
            throw new GameTestAssertException("computer exposes no energy storage capability");
        }
        return storage.getEnergyStored();
    }

    private static ComputerBlockEntity computer(final GameTestHelper helper) {
        return helper.getBlockEntity(COMPUTER_POS);
    }

    private VirtualMachineTests() {
    }
}

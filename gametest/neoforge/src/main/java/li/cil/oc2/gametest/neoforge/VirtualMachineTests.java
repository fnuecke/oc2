/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.serialization.NBTSerialization;
import li.cil.oc2.common.vm.Terminal;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
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
    // Generous because a passing test stops as soon as the milestone appears, so the bound only
    // costs time when something is actually broken.
    private static final int BOOT_TIMEOUT_TICKS = 150000;

    // Tests within a batch run simultaneously, batches run one after another. The two tests that
    // wait on real boot progress get a batch each: five emulators competing for cores made the
    // milestone miss its deadline intermittently.
    private static final String BOOT_BATCH = "oc2_boot";
    private static final String BOOT_RELOAD_BATCH = "oc2_boot_reload";

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

                final var registries = helper.getLevel().registryAccess();
                saved[0] = computer(helper).saveWithFullMetadata(registries);
                if (!saved[0].contains("state")) {
                    throw new GameTestAssertException("saved tag carries no VM state");
                }

                // Immediate load to make sure VM doesn't tick; RAM and drive blob files
                // could desync otherwise. Open todo to see if we can snapshot those...
                computer(helper).loadWithComponents(saved[0], registries);
            })
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

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_BATCH)
    public static void computerBootsGuestKernel(final GameTestHelper helper) {
        placeMachine(helper);
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(helper))
            .thenExecuteAfter(20, () -> computer(helper).start())
            .thenWaitUntil(() -> requireBooted(helper))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_RELOAD_BATCH)
    public static void bootedGuestSurvivesSaveAndLoad(final GameTestHelper helper) {
        placeMachine(helper);

        final CompoundTag[] saved = new CompoundTag[1];
        final long[] instructionsAtReload = new long[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(helper))
            .thenExecuteAfter(20, () -> computer(helper).start())
            .thenWaitUntil(() -> requireBooted(helper))
            .thenExecute(() -> {
                final var registries = helper.getLevel().registryAccess();
                saved[0] = computer(helper).saveWithFullMetadata(registries);

                // Immediate load to make sure VM doesn't tick; RAM and drive blob files
                // could desync otherwise. Open todo to see if we can snapshot those...
                computer(helper).loadWithComponents(saved[0], registries);
                instructionsAtReload[0] = guestInstructions(helper);
            })
            .thenWaitUntil(() -> {
                assertNoPanic(helper);
                if (computer(helper).getVirtualMachine().getRunState() != VMRunState.RUNNING) {
                    throw new GameTestAssertException("computer stopped after reload");
                }
                if (guestInstructions(helper) <= instructionsAtReload[0]) {
                    throw new GameTestAssertException("guest is not retiring instructions after reload");
                }
            })
            .thenSucceed();
    }

    // ------------------------------------------------------------- //

    private static void requireBooted(final GameTestHelper helper) {
        assertNoPanic(helper);
        final String text = screen(helper);
        if (!text.contains("Mounted root") || !text.contains("Run /sbin/init")) {
            throw new GameTestAssertException("guest has not handed off to userspace; screen:\n" + text);
        }
    }

    private static long guestInstructions(final GameTestHelper helper) {
        return ((AbstractVirtualMachine) computer(helper).getVirtualMachine())
            .state.board.getCpu().getInstructionsRetired();
    }

    private static void assertNoPanic(final GameTestHelper helper) {
        final String text = screen(helper);
        for (final String marker : new String[]{"Kernel panic", "Oops", "BUG:", "Call Trace"}) {
            if (text.contains(marker)) {
                throw new GameTestAssertException("guest reported '" + marker + "':\n" + text);
            }
        }
    }

    private static String screen(final GameTestHelper helper) {
        final CompoundTag tag = NBTSerialization.serialize(computer(helper).getTerminal());
        final byte[] buffer = tag.getByteArray("buffer");
        final StringBuilder text = new StringBuilder();
        for (int row = 0; row < Terminal.HEIGHT; row++) {
            for (int column = 0; column < Terminal.WIDTH; column++) {
                final int index = row * Terminal.WIDTH + column;
                final byte value = index < buffer.length ? buffer[index] : 0;
                text.append(value == 0 ? ' ' : (char) (value & 0xFF));
            }
            text.append('\n');
        }
        return text.toString();
    }

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

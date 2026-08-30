/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.gametest.ComputerFixture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class VirtualMachineTests {
    private static final int BOOT_TIMEOUT_TICKS = 150000;

    // Tests within a batch run simultaneously, batches run one after another. The two tests that
    // wait on real boot progress get a batch each. Since tests tick at ludicrous speeds, running
    // too many VMs at once makes the tests flaky.
    private static final String BOOT_BATCH = "oc2_boot";
    private static final String BOOT_RELOAD_BATCH = "oc2_boot_reload";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void computerWithHardwareRunsAndStops(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> computer.assertRunState(VMRunState.RUNNING, "after start"))
            .thenExecute(computer::stop)
            .thenExecuteAfter(60, () -> computer.assertRunState(VMRunState.STOPPED, "after stop"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void computerWithoutProcessorRefusesToStart(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer
                .install(DeviceTypes.FLASH_MEMORY.get(), new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.HARD_DRIVE.get(), new ItemStack(Items.HARD_DRIVE_CUSTOM.get())))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(60, () -> {
                computer.assertRunState(VMRunState.STOPPED, "without a processor");
                if (computer.virtualMachine().getBootError() == null) {
                    throw new GameTestAssertException("a machine with no processor must say so, not fail silently");
                }
            })
            .thenExecute(() -> computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get())))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> computer.assertRunState(VMRunState.RUNNING, "once a processor is installed"))
            .thenExecute(computer::stop)
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void runningComputerConsumesEnergy(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);

        final long[] charged = new long[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(200, () -> {
                charged[0] = computer.energy();
                if (charged[0] <= 0) {
                    throw new GameTestAssertException("computer never charged from the creative source");
                }
                breakBlock(helper, POWER_POS);
            })
            .thenExecuteAfter(120, () -> {
                final long drained = computer.energy();
                if (drained >= charged[0]) {
                    throw new GameTestAssertException("energy did not drain once the source was removed: "
                        + charged[0] + " -> " + drained);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void runningStateSurvivesNbtRoundTrip(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> {
                computer.assertRunState(VMRunState.RUNNING, "precondition");

                final CompoundTag saved = computer.save();
                if (!saved.contains("state")) {
                    throw new GameTestAssertException("saved tag carries no VM state");
                }

                // Immediate load to make sure VM doesn't tick; RAM and drive blob files
                // could desync otherwise. Open todo to see if we can snapshot those...
                computer.load(saved);
            })
            .thenExecuteAfter(200, () -> {
                computer.assertRunState(VMRunState.RUNNING, "after the round trip");
                computer.assertNoError();
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_BATCH)
    public static void computerBootsGuestKernel(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireBooted(computer))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_RELOAD_BATCH)
    public static void bootedGuestSurvivesSaveAndLoad(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);

        final long[] instructionsAtReload = new long[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireBooted(computer))
            .thenExecute(() -> {
                // Immediate load to make sure VM doesn't tick; RAM and drive blob files
                // could desync otherwise. Open todo to see if we can snapshot those...
                computer.load(computer.save());
                instructionsAtReload[0] = computer.guestInstructions();
            })
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                computer.assertRunState(VMRunState.RUNNING, "after reload");
                if (computer.guestInstructions() <= instructionsAtReload[0]) {
                    throw new GameTestAssertException("guest is not retiring instructions after reload");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void requireBooted(final ComputerFixture computer) {
        computer.assertNoGuestPanic();
        final String text = computer.screen();
        if (!text.contains("Mounted root") || !text.contains("Run /sbin/init")) {
            throw new GameTestAssertException("guest has not handed off to userspace; screen:\n" + text);
        }
    }

    private static ComputerFixture placeMachine(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);
        return computer;
    }

    private static void installHardware(final ComputerFixture computer) {
        computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
            .install(DeviceTypes.FLASH_MEMORY.get(), new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()))
            .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
            .install(DeviceTypes.HARD_DRIVE.get(), new ItemStack(Items.HARD_DRIVE_CUSTOM.get()));
    }

    // --------------------------------------------------------------------- //

    private VirtualMachineTests() {
    }
}

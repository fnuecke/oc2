/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.bus.device.item.MemoryDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.AbstractVirtualMachine;
import li.cil.oc2.common.vm.VMRunState;
import li.cil.oc2.common.vm.arch.R5Architecture;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.fixture.MachineFixture;
import li.cil.oc2.gametest.fixture.RobotFixture;
import li.cil.sedna.api.Sizes;
import li.cil.sedna.api.memory.MemoryAccessException;
import li.cil.sedna.riscv.R5Board;
import li.cil.sedna.riscv.R5MemoryRangeAllocationStrategy;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class VirtualMachineTests {
    private static final String BOOT_RELOAD_BATCH = "oc2_boot_reload";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void computerNeedsAProcessorToRun(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer
                .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.SEDNA_LINUX.getId())))
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
            .thenExecuteAfter(60, () -> computer.assertRunState(VMRunState.STOPPED, "after stop"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void removingProcessorStopsComputer(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> {
                computer.assertRunState(VMRunState.RUNNING, "precondition");
                computer.uninstall(DeviceTypes.CPU.get());
            })
            .thenExecuteAfter(60, () -> {
                computer.assertRunState(VMRunState.STOPPED, "after the processor was removed");
                computer.assertBootError(Constants.COMPUTER_ERROR_MISSING_CPU, "a machine losing its processor must say so");
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void swappingProcessorStopsComputer(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> {
                computer.assertRunState(VMRunState.RUNNING, "precondition");
                computer.uninstall(DeviceTypes.CPU.get());
                computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_Z80.get()));
            })
            .thenExecuteAfter(60, () -> {
                computer.assertRunState(VMRunState.STOPPED, "after the processor was swapped");
                computer.assertBootError(Constants.COMPUTER_ERROR_STATE_LOST, "a machine losing its state to a new architecture must say so");
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void unrecoverableTrapStopsComputer(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(300, () -> {
                computer.assertRunState(VMRunState.RUNNING, "precondition");
                // Wipes the firmware, so the next machine level trap faults in its handler.
                zeroMemory(computer, R5MemoryRangeAllocationStrategy.PHYSICAL_MEMORY_FIRST, 64 * 1024);
            })
            .thenExecuteAfter(60, () -> {
                computer.assertRunState(VMRunState.STOPPED, "after the trap handler was wiped");
                computer.assertBootError(Constants.COMPUTER_ERROR_HALTED, "a halted processor must say so");
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 900)
    public static void runningComputerConsumesEnergy(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        final long[] charged = new long[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
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

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_RELOAD_BATCH)
    public static void bootedGuestSurvivesSaveAndLoad(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        final long[] instructionsAtReload = new long[1];
        final List<String> memoryAtReload = new ArrayList<>();
        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireBooted(computer))
            .thenExecute(() -> {
                memoryAtReload.addAll(memory(computer));
                computer.load(computer.save());
                instructionsAtReload[0] = computer.guestInstructions();
            })
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                computer.assertRunState(VMRunState.RUNNING, "after reload");
                if (computer.guestInstructions() <= instructionsAtReload[0]) {
                    throw new GameTestAssertException("guest is not retiring instructions after reload");
                }
                if (!memory(computer).equals(memoryAtReload)) {
                    throw new GameTestAssertException("memory did not survive reload: "
                        + memoryAtReload + " -> " + memory(computer));
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_RELOAD_BATCH)
    public static void runningRobotKeepsMemoryOnSaveAndLoad(final GameTestHelper helper) {
        final RobotFixture robot = RobotFixture.place(helper, ROBOT_POS);

        final long[] instructionsAtReload = new long[1];
        final List<String> memoryAtReload = new ArrayList<>();
        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                robot.charge();
                Hardware.installLinux(robot);
            })
            .thenExecuteAfter(20, robot::start)
            .thenWaitUntil(() -> {
                robot.charge();
                robot.assertRunState(VMRunState.RUNNING, "before reload");
                if (robot.guestInstructions() == 0) {
                    throw new GameTestAssertException("guest has not started yet");
                }
            })
            .thenExecute(() -> {
                memoryAtReload.addAll(memory(robot));
                robot.entity().load(robot.entity().saveWithoutId(new CompoundTag()));
                instructionsAtReload[0] = robot.guestInstructions();
            })
            .thenWaitUntil(() -> {
                robot.charge();
                robot.assertNoGuestPanic();
                robot.assertRunState(VMRunState.RUNNING, "after reload");
                if (robot.guestInstructions() <= instructionsAtReload[0]) {
                    throw new GameTestAssertException("guest is not retiring instructions after reload");
                }
                if (!memory(robot).equals(memoryAtReload)) {
                    throw new GameTestAssertException("memory did not survive reload: "
                        + memoryAtReload + " -> " + memory(robot));
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void zeroMemory(final ComputerFixture computer, final long address, final int length) {
        try {
            final Field architectureField = AbstractVirtualMachine.class.getDeclaredField("architecture");
            architectureField.setAccessible(true);
            final Field boardField = R5Architecture.class.getDeclaredField("board");
            boardField.setAccessible(true);
            final R5Board board = (R5Board) boardField.get(architectureField.get(computer.virtualMachine()));
            for (int offset = 0; offset < length; offset += 8) {
                board.getMemoryMap().store(address + offset, 0, Sizes.SIZE_64_LOG2);
            }
        } catch (final ReflectiveOperationException | MemoryAccessException e) {
            throw new GameTestAssertException("could not wipe guest memory: " + e);
        }
    }

    private static List<String> memory(final MachineFixture machine) {
        return machine.devices().stream()
            .filter(device -> device instanceof MemoryDevice)
            .map(device -> device.serializeNBT().toString())
            .sorted()
            .toList();
    }

    private static void requireBooted(final ComputerFixture computer) {
        computer.assertNoGuestPanic();
        final String text = computer.screen();
        if (!text.contains("Mounted root") || !text.contains("Run /sbin/init")) {
            throw new GameTestAssertException("guest has not handed off to userspace; screen:\n" + text);
        }
    }

    // --------------------------------------------------------------------- //

    private VirtualMachineTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import static li.cil.oc2.gametest.TestSupport.*;

public final class ChunkUnloadTests {
    public static void runningMachineSurvivesChunkUnload(final GameTestHelper helper) {
        final ComputerFixture computer = placeMachine(helper);

        final long[] instructionsAtUnload = new long[1];
        final String[] persistedKeys = new String[1];
        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenExecuteAfter(600, () -> {
                computer.assertRunState(VMRunState.RUNNING, "precondition");
                instructionsAtUnload[0] = computer.guestInstructions();

                persistedKeys[0] = unloadAndReload(helper, computer);
            })
            .thenWaitUntil(() -> computer.assertRunState(VMRunState.RUNNING, "after reload"))
            .thenExecute(() -> {
                computer.assertNoError();
                final long instructions = computer.guestInstructions();
                if (instructions < instructionsAtUnload[0]) {
                    throw new GameTestAssertException("machine started cold instead of resuming: retired "
                        + instructions + " instructions, had " + instructionsAtUnload[0] + " before the unload;"
                        + " the unloaded block entity persisted " + persistedKeys[0]);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static String unloadAndReload(final GameTestHelper helper, final ComputerFixture computer) {
        final ServerLevel level = helper.getLevel();
        final BlockPos pos = helper.absolutePos(computer.pos());

        // Mimics regular MC behavior on chunk unload then load same equivalent order of operations.

        final ComputerBlockEntity unloaded = computer.blockEntity();
        unloaded.handleChunkUnloaded();
        final CompoundTag saved = unloaded.saveWithFullMetadata(level.registryAccess());

        level.removeBlockEntity(pos);

        final BlockEntity reloaded = BlockEntity.loadStatic(pos, level.getBlockState(pos), saved, level.registryAccess());
        assertNotNull(helper, reloaded, "block entity built from the saved tag");
        level.setBlockEntity(reloaded);

        return saved.getCompound("state").getAllKeys().toString();
    }

    private static ComputerFixture placeMachine(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);
        return computer;
    }

    private static void installHardware(final ComputerFixture computer) {
        computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
            .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
            .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
            .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()));
    }

    // --------------------------------------------------------------------- //

    private ChunkUnloadTests() {
    }
}

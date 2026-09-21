/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.RedstoneInterfaceBlockEntity;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.device.GuestTestDevices;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.GuestTests;
import li.cil.oc2.gametest.util.BusCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class RedstoneEventTests {
    private static final String BATCH = "oc2_redstone_events";
    private static final String SUITE = "redstone_events";

    private static final BlockPos SIGNAL_POS = DEVICE_POS.above();

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestReceivesInputChanges(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.EAST);
        place(helper, player, new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);

        final GuestTests tests = computer.guestTests();

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer
                .install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
                .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()))
                .install(DeviceTypes.CARD.get(), new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get())))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireReady();
            })
            .thenExecute(() -> tests.run(SUITE))
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                assertTrue(helper, "the guest did not signal that it is listening",
                    redstoneInterface(helper).getOutputForDirection(Direction.SOUTH) == 15);
            })
            .thenExecute(() -> helper.setBlock(SIGNAL_POS, Blocks.REDSTONE_BLOCK))
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                assertTrue(helper, "the guest did not acknowledge the signal",
                    redstoneInterface(helper).getOutputForDirection(Direction.SOUTH) == 14);
            })
            .thenExecute(() -> helper.setBlock(SIGNAL_POS, Blocks.AIR))
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static RedstoneInterfaceBlockEntity redstoneInterface(final GameTestHelper helper) {
        return helper.getBlockEntity(DEVICE_POS);
    }

    // --------------------------------------------------------------------- //

    private RedstoneEventTests() {
    }
}

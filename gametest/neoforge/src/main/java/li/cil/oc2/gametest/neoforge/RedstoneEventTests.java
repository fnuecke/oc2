/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.blockentity.BusCableBlockEntity;
import li.cil.oc2.common.blockentity.RedstoneInterfaceBlockEntity;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.device.GuestTestDevices;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.GuestTests;
import li.cil.oc2.gametest.fixture.Hardware;
import li.cil.oc2.gametest.util.BusCables;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestSequence;
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
    private static final String MLAPI_BATCH = "oc2_redstone_mlapi_events";
    private static final String MLAPI_SUITE = "mlapi_events";
    private static final String MLAPI_NAMES_BATCH = "oc2_mlapi_names";
    private static final String MLAPI_NAMES_SUITE = "mlapi_names";

    private static final BlockPos SIGNAL_POS = DEVICE_POS.above();

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestReceivesInputChanges(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerWithRedstoneInterface(helper);
        final GuestTests tests = computer.guestTests();

        startSequence(helper, computer, tests, SUITE)
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

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = MLAPI_BATCH)
    public static void guestExampleWaitsForInputChange(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerWithRedstoneInterface(helper);
        final GuestTests tests = computer.guestTests();

        startSequence(helper, computer, tests, MLAPI_SUITE)
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                assertTrue(helper, "the guest did not start the example",
                    redstoneInterface(helper).getOutputForDirection(Direction.SOUTH) == 15);
            })
            .thenExecute(() -> helper.setBlock(SIGNAL_POS, Blocks.REDSTONE_BLOCK))
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = MLAPI_NAMES_BATCH)
    public static void guestExampleListsInterfaceLabel(final GameTestHelper helper) {
        final ComputerFixture computer = placeComputerWithRedstoneInterface(helper);
        final GuestTests tests = computer.guestTests();
        final BusCableBlockEntity cable = helper.getBlockEntity(CABLE_POS);
        cable.setInterfaceName(Direction.EAST, "test_device");

        startSequence(helper, computer, tests, MLAPI_NAMES_SUITE)
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ComputerFixture placeComputerWithRedstoneInterface(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.placePowered(helper, player);
        BusCables.placeCableWithInterfaces(helper, player, CABLE_POS, Direction.WEST, Direction.EAST);
        place(helper, player, new ItemStack(Items.REDSTONE_INTERFACE.get()), DEVICE_POS);
        return computer;
    }

    private static GameTestSequence startSequence(final GameTestHelper helper, final ComputerFixture computer, final GuestTests tests, final String suite) {
        return helper.startSequence()
            .thenExecuteAfter(20, () -> {
                Hardware.installLinuxWithExtraMemory(computer);
                computer.install(DeviceTypes.CARD.get(), new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get()));
            })
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireReady();
            })
            .thenExecute(() -> tests.run(suite));
    }

    private static RedstoneInterfaceBlockEntity redstoneInterface(final GameTestHelper helper) {
        return helper.getBlockEntity(DEVICE_POS);
    }

    // --------------------------------------------------------------------- //

    private RedstoneEventTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.ComputerFixture;
import li.cil.oc2.gametest.GuestTests;
import li.cil.oc2.gametest.device.GuestTestDevices;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuestLibraryTests {
    private static final int BOOT_TIMEOUT_TICKS = 300000;

    private static final String BATCH = "oc2_guest_libraries";
    private static final String SUITE = "guest_libraries";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestLibrariesPassTheirOwnTests(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        final GuestTests tests = computer.guestTests();

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer
                .install(DeviceTypes.FLASH_MEMORY, new ItemStack(Items.FLASH_MEMORY_CUSTOM.get()))
                .install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.MEMORY, new ItemStack(Items.MEMORY_LARGE.get()))
                .install(DeviceTypes.HARD_DRIVE, new ItemStack(Items.HARD_DRIVE_CUSTOM.get()))
                .install(DeviceTypes.CARD, new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get())))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireReady();
            })
            .thenExecute(() -> tests.run(SUITE))
            .thenWaitUntil(() -> {
                computer.assertNoGuestPanic();
                tests.requireSuccess();
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private GuestLibraryTests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.gametest.device.GuestTestDevices;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.GuestTests;
import li.cil.oc2.gametest.fixture.Hardware;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuestLibraryTests {
    private static final String BATCH = "oc2_guest_libraries";
    private static final String SUITE = "guest_libraries";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestLibrarySuitePasses(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.placePowered(helper, player);

        final GuestTests tests = computer.guestTests();

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                Hardware.installLinuxWithExtraMemory(computer);
                computer.install(DeviceTypes.CARD.get(), new ItemStack(GuestTestDevices.GUEST_TEST_PORT.get()));
            })
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

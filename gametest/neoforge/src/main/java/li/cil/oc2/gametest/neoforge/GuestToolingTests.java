/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.data.BlockDeviceDataRegistry;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.ComputerFixture;
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
public final class GuestToolingTests {
    private static final String BATCH = "oc2_guest_tooling";
    private static final String PROMPT = "# "; // Shell is up, as opposed to login still reading input.

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestReportsUsableDriveGeometry(final GameTestHelper helper) {
        final ComputerFixture computer = machine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, "login:"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> requireScreen(computer, PROMPT))
            .thenExecuteAfter(20, () -> computer.type("fdisk -l /dev/vda; echo GEOMETRY''-END\n"))
            .thenWaitUntil(() -> requireScreen(computer, "GEOMETRY-END"))
            .thenExecute(() -> {
                final String screen = computer.screen();
                requireContains(screen, "16 heads", "guest did not get the advertised head count");
                requireContains(screen, "63 sectors/track", "guest did not get the advertised sector count");
                requireContains(screen, "cylinders", "fdisk printed no cylinder count");
                if (screen.contains(" 0 cylinders")) {
                    throw new GameTestAssertException(
                        "drive reports zero cylinders, so it cannot be partitioned:\n" + screen);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestLoadsShippedKeymaps(final GameTestHelper helper) {
        final ComputerFixture computer = machine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, "login:"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> requireScreen(computer, PROMPT))
            .thenExecuteAfter(20, () -> computer.type(
                "loadkmap < /usr/share/keymaps/de.bmap && echo KEYMAP''-OK || echo KEYMAP''-FAIL\n"))
            .thenWaitUntil(() -> requireScreen(computer, "KEYMAP-OK", "KEYMAP-FAIL"))
            .thenExecute(() -> {
                final String screen = computer.screen();
                if (screen.contains("KEYMAP-FAIL")) {
                    throw new GameTestAssertException("loadkmap rejected a shipped keymap:\n" + screen);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BATCH)
    public static void guestCanUseASecondDriveAsSwap(final GameTestHelper helper) {
        final ComputerFixture computer = machine(helper);

        helper.startSequence()
            .thenExecuteAfter(20, () -> installHardware(computer)
                .install(DeviceTypes.HARD_DRIVE.get(), new ItemStack(Items.HARD_DRIVE_SMALL.get())))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, "login:"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> requireScreen(computer, PROMPT))
            .thenExecuteAfter(20, () -> computer.type(
                "mkswap /dev/vdb >/dev/null && swapon /dev/vdb && grep -q vdb /proc/swaps"
                    + " && echo SWAP''-OK || echo SWAP''-FAIL\n"))
            .thenWaitUntil(() -> requireScreen(computer, "SWAP-OK", "SWAP-FAIL"))
            .thenExecute(() -> {
                final String screen = computer.screen();
                if (screen.contains("SWAP-FAIL")) {
                    throw new GameTestAssertException("guest could not enable swap:\n" + screen);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ComputerFixture machine(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);
        return computer;
    }

    private static ComputerFixture installHardware(final ComputerFixture computer) {
        return computer.install(DeviceTypes.CPU.get(), new ItemStack(Items.CPU_RISCV.get()))
            .install(DeviceTypes.FLASH_MEMORY.get(), Items.FLASH_MEMORY.get().withData(BlockDeviceDataRegistry.FIRMWARE_RISCV.getId()))
            .install(DeviceTypes.MEMORY.get(), new ItemStack(Items.MEMORY_LARGE.get()))
            .install(DeviceTypes.HARD_DRIVE.get(), Items.HARD_DRIVE_LARGE.get().withData(BlockDeviceDataRegistry.BUILDROOT.getId()));
    }

    private static void requireScreen(final ComputerFixture computer, final String... anyOf) {
        computer.assertNoGuestPanic();
        final String screen = computer.screen();
        for (final String marker : anyOf) {
            if (screen.contains(marker)) {
                return;
            }
        }
        throw new GameTestAssertException("waiting for " + String.join(" or ", anyOf)
            + "; " + computer.describe() + "\n" + screen);
    }

    private static void requireContains(final String screen, final String marker, final String what) {
        if (!screen.contains(marker)) {
            throw new GameTestAssertException(what + "; screen:\n" + screen);
        }
    }

    // --------------------------------------------------------------------- //

    private GuestToolingTests() {
    }
}

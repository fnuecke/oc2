/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.item.SoundCardDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.Hardware;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.testframework.annotation.ForEachTest;
import net.neoforged.testframework.annotation.TestHolder;
import net.neoforged.testframework.gametest.GameTest;

import java.util.regex.Pattern;

import static li.cil.oc2.gametest.fixture.MachineFixture.LOGIN_PROMPT;
import static li.cil.oc2.gametest.fixture.MachineFixture.SHELL_PROMPT;
import static li.cil.oc2.gametest.util.TestSupport.*;

@ForEachTest(groups = "guest_tooling")
public final class GuestToolingTests {
    private static final String GEOMETRY_BATCH = "oc2_guest_tooling_geometry";
    private static final String SWAP_BATCH = "oc2_guest_tooling_swap";
    private static final String SOUND_BATCH = "oc2_guest_tooling_sound";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE_ID, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = GEOMETRY_BATCH)
    @TestHolder
    public static void guestReportsUsableDriveGeometry(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, LOGIN_PROMPT))
            .thenExecute(computer::loginAsRoot)
            .thenWaitUntil(() -> requireScreen(computer, SHELL_PROMPT))
            .thenExecuteAfter(20, () -> computer.type(script("fdisk -l /dev/vda; echo GEOMETRY:$?")))
            .thenWaitUntil(() -> requireScreen(computer, "GEOMETRY:0"))
            .thenExecute(() -> {
                final String screen = computer.screen();
                requireContains(screen, "16 heads", "guest did not get the advertised head count");
                requireContains(screen, "63 sectors/track", "guest did not get the advertised sector count");
                requireContains(screen, "cylinders", "fdisk printed no cylinder count");
                if (screen.contains(" 0 cylinders")) {
                    throw new GameTestAssertException(Component.literal(
                        "drive reports zero cylinders, so it cannot be partitioned:\n" + screen), 0);
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = SWAP_BATCH)
    @TestHolder
    public static void guestCanUseASecondDriveAsSwap(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                Hardware.installLinux(computer);
                computer.install(DeviceTypes.HARD_DRIVE.get(), new ItemStack(Items.HARD_DRIVE_SMALL.get()));
            })
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, LOGIN_PROMPT))
            .thenExecute(computer::loginAsRoot)
            .thenWaitUntil(() -> requireScreen(computer, SHELL_PROMPT))
            .thenExecuteAfter(20, () -> computer.type(script(
                "mkswap /dev/vdb >/dev/null && swapon /dev/vdb && grep -q vdb /proc/swaps; echo SWAP:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("SWAP"), "swap setup should report back"))
            .thenExecute(() -> computer.assertScreenContains("SWAP:0", "guest could not enable swap"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE_ID, timeoutTicks = REAL_TIME_TIMEOUT_TICKS, batch = SOUND_BATCH)
    @TestHolder
    public static void guestPlaysThroughTheSoundCard(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                Hardware.installLinux(computer);
                computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.SOUND_CARD.get()));
            })
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, LOGIN_PROMPT))
            .thenExecute(() -> {
                if (computer.devices().stream().noneMatch(SoundCardDevice.class::isInstance)) {
                    throw new GameTestAssertException(Component.literal("the sound card was not detected by a RISC-V machine"), 0);
                }
                computer.loginAsRoot();
            })
            .thenWaitUntil(() -> requireScreen(computer, SHELL_PROMPT))
            .thenExecuteAfter(20, () -> computer.type(script(
                "head -c 8000 /dev/urandom > /tmp/noise.raw; ls /dev/dsp /dev/snd/pcmC0D0p >/dev/null; echo DEVICES:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("DEVICES"), "the device check should report back"))
            .thenExecute(() -> computer.assertScreenContains("DEVICES:0", "the guest has no sound card"))
            .thenExecute(() -> computer.type(script("cat /tmp/noise.raw > /dev/dsp; echo FIRST:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("FIRST"), "playback should report back"))
            .thenExecute(() -> computer.assertScreenContains("FIRST:0", "playback failed"))
            .thenExecute(() -> requireAudioReachedTheCard(computer))
            .thenExecute(() -> computer.type(script("cat /tmp/noise.raw > /dev/dsp; echo SECOND:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("SECOND"), "the second playback should report back"))
            .thenExecute(() -> computer.assertScreenContains("SECOND:0", "playing a second time failed"))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static Pattern exitStatus(final String marker) {
        return Pattern.compile("^" + marker + ":\\d+ *$", Pattern.MULTILINE);
    }

    private static void requireScreen(final ComputerFixture computer, final String... anyOf) {
        computer.assertNoGuestPanic();
        final String screen = computer.screen();
        for (final String marker : anyOf) {
            if (screen.contains(marker)) {
                return;
            }
        }
        throw new GameTestAssertException(Component.literal("waiting for " + String.join(" or ", anyOf)
            + "; " + computer.describe() + "\n" + screen), 0);
    }

    private static void requireAudioReachedTheCard(final ComputerFixture computer) {
        final SoundCardDevice card = computer.devices().stream()
            .filter(SoundCardDevice.class::isInstance)
            .map(SoundCardDevice.class::cast)
            .findFirst()
            .orElseThrow(() -> new GameTestAssertException(Component.literal("the sound card is no longer on the bus"), 0));

        final SoundCardDevice.Chunk chunk = card.getStream().poll();
        if (chunk == null || chunk.samples().length == 0) {
            throw new GameTestAssertException(Component.literal("the guest played audio, but none of it reached the sound card"), 0);
        }
    }

    private static void requireContains(final String screen, final String marker, final String what) {
        if (!screen.contains(marker)) {
            throw new GameTestAssertException(Component.literal(what + "; screen:\n" + screen), 0);
        }
    }

    // --------------------------------------------------------------------- //

    private GuestToolingTests() {
    }
}

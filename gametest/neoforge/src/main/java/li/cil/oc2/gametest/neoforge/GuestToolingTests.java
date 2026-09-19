/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.common.bus.device.vm.item.SoundCardDevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.Hardware;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.regex.Pattern;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class GuestToolingTests {
    private static final String GEOMETRY_BATCH = "oc2_guest_tooling_geometry";
    private static final String SWAP_BATCH = "oc2_guest_tooling_swap";
    private static final String SOUND_BATCH = "oc2_guest_tooling_sound";

    private static final String PROMPT = "# ";

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = GEOMETRY_BATCH)
    public static void guestReportsUsableDriveGeometry(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> Hardware.installLinux(computer))
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, "login:"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> requireScreen(computer, PROMPT))
            .thenExecuteAfter(20, () -> computer.type(script("fdisk -l /dev/vda; echo GEOMETRY:$?")))
            .thenWaitUntil(() -> requireScreen(computer, "GEOMETRY:0"))
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

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = SWAP_BATCH)
    public static void guestCanUseASecondDriveAsSwap(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                Hardware.installLinux(computer);
                computer.install(DeviceTypes.HARD_DRIVE.get(), new ItemStack(Items.HARD_DRIVE_SMALL.get()));
            })
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, "login:"))
            .thenExecute(() -> computer.type("root\n"))
            .thenWaitUntil(() -> requireScreen(computer, PROMPT))
            .thenExecuteAfter(20, () -> computer.type(script(
                "mkswap /dev/vdb >/dev/null && swapon /dev/vdb && grep -q vdb /proc/swaps; echo SWAP:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("SWAP"), "swap setup should report back"))
            .thenExecute(() -> requireContains(computer.screen(), "SWAP:0", "guest could not enable swap"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = SOUND_BATCH)
    public static void guestPlaysThroughTheSoundCard(final GameTestHelper helper) {
        final ComputerFixture computer = ComputerFixture.placePowered(helper, COMPUTER_POS);

        helper.startSequence()
            .thenExecuteAfter(20, () -> {
                Hardware.installLinux(computer);
                computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.SOUND_CARD.get()));
            })
            .thenExecuteAfter(20, computer::start)
            .thenWaitUntil(() -> requireScreen(computer, "login:"))
            .thenExecute(() -> {
                if (computer.devices().stream().noneMatch(SoundCardDevice.class::isInstance)) {
                    throw new GameTestAssertException("the sound card was not detected by a RISC-V machine");
                }
                computer.type("root\n");
            })
            .thenWaitUntil(() -> requireScreen(computer, PROMPT))
            .thenExecuteAfter(20, () -> computer.type(script(
                "head -c 8000 /dev/urandom > /tmp/noise.raw; ls /dev/dsp /dev/snd/pcmC0D0p >/dev/null; echo DEVICES:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("DEVICES"), "the device check should report back"))
            .thenExecute(() -> requireContains(computer.screen(), "DEVICES:0", "the guest has no sound card"))
            .thenExecute(() -> computer.type(script("cat /tmp/noise.raw > /dev/dsp; echo FIRST:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("FIRST"), "playback should report back"))
            .thenExecute(() -> requireContains(computer.screen(), "FIRST:0", "playback failed"))
            .thenExecute(() -> requireAudioReachedTheCard(computer))
            .thenExecute(() -> computer.type(script("cat /tmp/noise.raw > /dev/dsp; echo SECOND:$?")))
            .thenWaitUntil(() -> computer.assertScreenMatches(exitStatus("SECOND"), "the second playback should report back"))
            .thenExecute(() -> requireContains(computer.screen(), "SECOND:0", "playing a second time failed"))
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
        throw new GameTestAssertException("waiting for " + String.join(" or ", anyOf)
            + "; " + computer.describe() + "\n" + screen);
    }

    private static void requireAudioReachedTheCard(final ComputerFixture computer) {
        final SoundCardDevice card = computer.devices().stream()
            .filter(SoundCardDevice.class::isInstance)
            .map(SoundCardDevice.class::cast)
            .findFirst()
            .orElseThrow(() -> new GameTestAssertException("the sound card is no longer on the bus"));

        final SoundCardDevice.Chunk chunk = card.getStream().poll();
        if (chunk == null || chunk.samples().length == 0) {
            throw new GameTestAssertException("the guest played audio, but none of it reached the sound card");
        }
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

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.fixture.Z80Fixture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class Z80Tests {
    private static final int BOOT_TIMEOUT_TICKS = 20000;

    private static final String BOOT_BATCH = "oc2_z80_boot";
    private static final String SYSTEM_DISK_BATCH = "oc2_z80_system_disk";
    private static final String DEVS_BATCH = "oc2_z80_devs";

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = BOOT_BATCH)
    public static void z80BootsToTheCpmPrompt(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, z80::install)
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = SYSTEM_DISK_BATCH)
    public static void theSystemDiskCarriesTheFilesOc2Ships(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, z80::install)
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenExecute(() -> z80.command("DIR"))
            .thenWaitUntil(() -> {
                z80.assertScreenContains("DEVS", "the emulator's own tools should survive composition");
                z80.assertScreenContains("ZMAC", "the assembler should survive composition");
                z80.assertScreenContains("ZML", "the linker should survive composition");
                z80.assertScreenContains("OCAPI", "oc2 should add its device API library");
                z80.assertScreenContains("REDSTN", "oc2 should add its example source");
            })
            .thenExecute(() -> z80.command("TYPE OCAPI.INC"))
            .thenWaitUntil(() -> z80.assertScreenContains("OCFIND", "the file's contents should read back"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = DEVS_BATCH)
    public static void devsListsTheMidLevelApiDevice(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, () -> z80.install().withRedstoneCard())
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenExecute(() -> z80.command("DEVS"))
            .thenWaitUntil(() -> z80.assertScreenContains("REDSTN",
                "the redstone card should enumerate through the device API port"))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private Z80Tests() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.api.bus.device.io.IOMethod;
import li.cil.oc2.gametest.fixture.Z80Fixture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class Z80Tests {
    private static final int BOOT_TIMEOUT_TICKS = 20000;

    private static final String BOOT_BATCH = "oc2_z80_boot";
    private static final String SYSTEM_DISK_BATCH = "oc2_z80_system_disk";
    private static final String DEVS_BATCH = "oc2_z80_devs";
    private static final String ITEMS_BATCH = "oc2_z80_items";

    private static final int GET_SLOTS_CODE = 2;
    private static final int GET_ITEM_NAME_CODE = 4;

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

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = ITEMS_BATCH)
    public static void devsListsAnInventoryOnTheBus(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, z80::install)
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenExecute(() -> z80.command("DEVS"))
            .thenWaitUntil(() -> z80.assertScreenContains("ITEMS",
                "an inventory on the bus should enumerate through the device API port"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = ITEMS_BATCH)
    public static void anInventoryOnTheBusReadsBackThroughTheDeviceApi(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, () -> z80.install().withInventory())
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenExecute(() -> assertChestIsReadable(z80))
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void assertChestIsReadable(final Z80Fixture z80) {
        final StringBuilder seen = new StringBuilder();
        for (final Device device : z80.computer().devices()) {
            if (!(device instanceof final IODevice io) || !"ITEMS".equals(io.getIOName())) {
                continue;
            }

            final byte[] slots = invoke(io, GET_SLOTS_CODE, 0, 1);
            final int id = (slots[0] & 0xFF) | ((slots[1] & 0xFF) << 8);
            final int count = slots[2] & 0xFF;
            seen.append(" id=").append(id).append(" count=").append(count);
            if (count != 42) {
                continue;
            }

            final String name = new String(invoke(io, GET_ITEM_NAME_CODE, id & 0xFF, id >>> 8), US_ASCII);
            if (!"minecraft:redstone".equals(name)) {
                throw new GameTestAssertException("slot 0 of the chest resolved to [" + name + "]");
            }
            return;
        }

        throw new GameTestAssertException("no ITEMS device on the bus reported the chest's contents;"
            + " saw" + (seen.isEmpty() ? " none" : seen));
    }

    private static byte[] invoke(final IODevice device, final int code, final int... arguments) {
        final byte[] bytes = new byte[arguments.length];
        for (int i = 0; i < arguments.length; i++) {
            bytes[i] = (byte) arguments[i];
        }

        for (final IOMethod method : device.getIOMethods()) {
            if (method.getCode() != code) {
                continue;
            }
            final ByteArrayOutputStream results = new ByteArrayOutputStream();
            try {
                method.invoke(new ByteArrayInputStream(bytes), results);
            } catch (final Throwable e) {
                throw new GameTestAssertException("function " + code + " failed: " + e);
            }
            return results.toByteArray();
        }

        throw new GameTestAssertException("device has no function with code " + code);
    }

    private Z80Tests() {
    }
}

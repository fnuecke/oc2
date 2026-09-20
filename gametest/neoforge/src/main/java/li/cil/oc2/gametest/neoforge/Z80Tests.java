/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.bus.device.io.IODevice;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.gametest.fixture.Z80Fixture;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static li.cil.oc2.gametest.util.DeviceCalls.invokeIo;
import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class Z80Tests {
    private static final int BOOT_TIMEOUT_TICKS = 20000;
    private static final String DEVS_BATCH = "oc2_z80_devs";
    private static final String ITEMS_BATCH = "oc2_z80_items";
    private static final String SERIAL_BATCH = "oc2_z80_serial";

    private static final int GET_SLOTS_CODE = 2;
    private static final int GET_ITEM_NAME_CODE = 4;

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = SERIAL_BATCH)
    public static void z80EnumeratesSerialCard(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(20, () -> z80.install().withSerialCard())
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenExecute(() -> {
                final ItemStack card = z80.computer().slot(DeviceTypes.CARD.get());
                if (!SerialInterfaceCardItem.hasAddress(card)) {
                    throw new GameTestAssertException("a card without an address should get one when it mounts");
                }
            })
            .thenExecute(() -> z80.command("DEVS"))
            .thenWaitUntil(() -> z80.assertScreenContains("UART",
                "card should enumerate as a character device"))
            .thenExecute(() -> z80.command("TERM"))
            .thenWaitUntil(() -> z80.assertScreenContains("quits",
                "TERM should find the card and start, rather than saying there is none"))
            .thenExecute(() -> z80.computer().type("\u001d"))
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = BOOT_TIMEOUT_TICKS, batch = SERIAL_BATCH)
    public static void serchatUsesCardAddress(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final Z80Fixture z80 = Z80Fixture.place(helper, player);
        placePower(helper, player);

        final ItemStack card = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        SerialInterfaceCardItem.setAddress(card, 42);

        helper.startSequence()
            .thenExecuteAfter(20, () -> z80.install().computer().install(DeviceTypes.CARD.get(), card))
            .thenExecuteAfter(20, z80::start)
            .thenWaitUntil(() -> z80.assertScreenContains("A>", "CP/M should reach its prompt"))
            .thenExecute(() -> z80.command("ZMAC SERCHAT /E"))
            .thenWaitUntil(() -> z80.assertScreenContains("SERCHAT.Z80    assembled with   NO ERRORS", "the example should assemble"))
            .thenWaitUntil(() -> assertBackAtPrompt(z80))
            .thenExecuteAfter(20, () -> z80.command("ZML SERCHAT"))
            .thenExecuteAfter(20, () -> {
            })
            .thenWaitUntil(() -> assertBackAtPrompt(z80))
            .thenExecuteAfter(20, () -> z80.command(""))
            .thenExecuteAfter(20, () -> z80.command("SERCHAT 7"))
            .thenWaitUntil(() -> z80.assertScreenContains("This is endpoint 42",
                "SERCHAT should take its address from the card"))
            .thenExecute(() -> z80.command(""))
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
    public static void devsListsItemsDevice(final GameTestHelper helper) {
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
    public static void itemsDeviceReadsChest(final GameTestHelper helper) {
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

            final byte[] slots = invokeIo(io, GET_SLOTS_CODE, 0, 1);
            final int id = (slots[0] & 0xFF) | ((slots[1] & 0xFF) << 8);
            final int count = slots[2] & 0xFF;
            seen.append(" id=").append(id).append(" count=").append(count);
            if (count != 42) {
                continue;
            }

            final String name = new String(invokeIo(io, GET_ITEM_NAME_CODE, id & 0xFF, id >>> 8), US_ASCII);
            if (!"minecraft:redstone".equals(name)) {
                throw new GameTestAssertException("slot 0 of the chest resolved to [" + name + "]");
            }
            return;
        }

        throw new GameTestAssertException("no ITEMS device on the bus reported the chest's contents;"
            + " saw" + (seen.isEmpty() ? " none" : seen));
    }

    private static void assertBackAtPrompt(final Z80Fixture z80) {
        if (!z80.screen().stripTrailing().endsWith("A>")) {
            throw new GameTestAssertException("CP/M should be back at its prompt");
        }
    }

    private Z80Tests() {
    }
}

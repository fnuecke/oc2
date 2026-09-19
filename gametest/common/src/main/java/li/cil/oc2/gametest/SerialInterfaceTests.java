/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.item.SerialInterfaceCardItem;
import li.cil.oc2.common.serial.SerialFrame;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.ConnectorFixture;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.nio.charset.StandardCharsets;

import static li.cil.oc2.gametest.util.TestSupport.fakePlayer;
import static li.cil.oc2.gametest.util.TestSupport.placePower;

public final class SerialInterfaceTests {
    private static final byte[] PEER_MAC = {0x02, 0x6F, 0x63, 0x7E, 0x7E, 0x7E};
    private static final int DEFAULT_DIVISOR = 12;

    // --------------------------------------------------------------------- //

    public static void connectorResolvesSerialCard(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        player.setXRot(90);
        final ConnectorFixture connector = ConnectorFixture.place(helper, player, computer.pos().above());

        helper.startSequence()
            .thenExecuteAfter(40, () -> computer.install(DeviceTypes.CARD.get(),
                new ItemStack(Items.SERIAL_INTERFACE_CARD.get())))
            .thenExecuteAfter(80, () -> {
                final NetworkInterface card = computer.networkInterface(Direction.UP);
                if (card == null) {
                    throw new GameTestAssertException(
                        "an installed serial interface card is not reachable as a block capability");
                }

                final Object resolved = connector.adjacentInterface();
                if (resolved != card) {
                    throw new GameTestAssertException(
                        "the connector did not resolve the computer's serial card (resolved "
                            + resolved + ", card " + card + ")");
                }
            })
            .thenSucceed();
    }

    public static void disabledSideExposesNoInterface(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        final ItemStack card = new ItemStack(Items.SERIAL_INTERFACE_CARD.get());
        SerialInterfaceCardItem.setSideConfiguration(card, Direction.UP, false);

        helper.startSequence()
            .thenExecuteAfter(40, () -> computer.install(DeviceTypes.CARD.get(), card))
            .thenExecuteAfter(80, () -> {
                if (computer.networkInterface(Direction.UP) != null) {
                    throw new GameTestAssertException("a side configured off should carry nothing");
                }
                if (computer.networkInterface(Direction.NORTH) == null) {
                    throw new GameTestAssertException("a side left on should carry traffic");
                }
            })
            .thenSucceed();
    }

    public static void cardDoesNotEchoItsOwnFrame(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(40, () -> computer.install(DeviceTypes.CARD.get(),
                new ItemStack(Items.SERIAL_INTERFACE_CARD.get())))
            .thenExecuteAfter(80, () -> {
                final NetworkInterface card = computer.networkInterface(Direction.NORTH);
                if (card == null) {
                    throw new GameTestAssertException("the card is not on the bus");
                }

                final byte[] data = "ping".getBytes(StandardCharsets.UTF_8);
                final byte[] frame = new SerialFrame(PEER_MAC, DEFAULT_DIVISOR, data).toEthernetFrame();
                card.writeEthernetFrame(card, frame, 12);

                if (card.readEthernetFrame() != null) {
                    throw new GameTestAssertException(
                        "a serial card must not put back on the wire what it just heard");
                }
            })
            .thenSucceed();
    }

    public static void twoSerialCardsFitOneComputer(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.SERIAL_INTERFACE_CARD.get()));
                computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.SERIAL_INTERFACE_CARD.get()));
            })
            .thenExecuteAfter(80, () -> {
                computer.assertNoBootError();
                if (computer.networkInterface(Direction.NORTH) == null) {
                    throw new GameTestAssertException("neither card came up");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private SerialInterfaceTests() {
    }
}

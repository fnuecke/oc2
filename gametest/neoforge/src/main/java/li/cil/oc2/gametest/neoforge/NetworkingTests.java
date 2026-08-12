/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import li.cil.oc2.common.blockentity.NetworkHubBlockEntity;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import javax.annotation.Nullable;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkingTests {
    private static final BlockPos HUB_A = new BlockPos(2, WORK_Y, 2);
    private static final BlockPos HUB_B = new BlockPos(8, WORK_Y, 2);
    private static final BlockPos CONNECTOR_A = HUB_A.above();
    private static final BlockPos CONNECTOR_B = HUB_B.above();

    private static final byte[] FRAME = new byte[64];

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void framesFromTheLocalInterfaceReachThePeer(final GameTestHelper helper) {
        placeLink(helper);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final NetworkHubBlockEntity hubB = helper.getBlockEntity(HUB_B);
                    final int before = framesReceived(hubB);

                    connectorInterface(helper, CONNECTOR_A)
                            .writeEthernetFrame(helper.getBlockEntity(HUB_A), FRAME, 12);

                    final int after = framesReceived(hubB);
                    if (after <= before) {
                        throw new GameTestAssertException(
                                "a frame sent from the connector's own adjacent interface never reached "
                                        + "the far end of the cable (hub frame count " + before + " -> " + after + ")");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void framesFromThePeerReachTheLocalInterface(final GameTestHelper helper) {
        placeLink(helper);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final NetworkHubBlockEntity hubA = helper.getBlockEntity(HUB_A);
                    final int before = framesReceived(hubA);

                    connectorInterface(helper, CONNECTOR_A)
                            .writeEthernetFrame(connectorInterface(helper, CONNECTOR_B), FRAME, 12);

                    if (framesReceived(hubA) <= before) {
                        throw new GameTestAssertException(
                                "a frame arriving over the cable was not delivered to the local block");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void framesAreNotEchoedBackToTheirSource(final GameTestHelper helper) {
        placeLink(helper);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final NetworkHubBlockEntity hubA = helper.getBlockEntity(HUB_A);
                    final int before = framesReceived(hubA);

                    connectorInterface(helper, CONNECTOR_A)
                            .writeEthernetFrame(helper.getBlockEntity(HUB_A), FRAME, 12);

                    if (framesReceived(hubA) != before) {
                        throw new GameTestAssertException(
                                "a frame was echoed back to the interface it came from");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void expiredFramesAreDropped(final GameTestHelper helper) {
        placeLink(helper);

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    final NetworkHubBlockEntity hubB = helper.getBlockEntity(HUB_B);
                    final int before = framesReceived(hubB);

                    connectorInterface(helper, CONNECTOR_A)
                            .writeEthernetFrame(helper.getBlockEntity(HUB_A), FRAME, 0);

                    if (framesReceived(hubB) != before) {
                        throw new GameTestAssertException("a frame with no time to live was forwarded");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aConnectorResolvesTheNetworkCardOfTheComputerItIsOn(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), POWER_POS);

        player.setXRot(90);
        place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), COMPUTER_POS.above());

        helper.startSequence()
                .thenExecuteAfter(40, () -> {
                    if (computerNetworkInterface(helper) != null) {
                        throw new GameTestAssertException(
                                "a computer with no network card must not expose a network interface");
                    }

                    final ItemStack leftover = ((ComputerBlockEntity) helper.getBlockEntity(COMPUTER_POS))
                            .getItemStackHandlers()
                            .getItemHandler(DeviceTypes.CARD)
                            .orElseThrow(() -> new GameTestAssertException("no card slot"))
                            .insertItem(0, new ItemStack(Items.NETWORK_INTERFACE_CARD.get()), false);
                    if (!leftover.isEmpty()) {
                        throw new GameTestAssertException("could not install the network card");
                    }
                })
                .thenExecuteAfter(80, () -> {
                    final NetworkInterface card = computerNetworkInterface(helper);
                    if (card == null) {
                        throw new GameTestAssertException(
                                "an installed network card is not reachable as a block capability");
                    }

                    final Object resolved = adjacentInterfaceOf(helper, COMPUTER_POS.above());
                    if (resolved != card) {
                        throw new GameTestAssertException(
                                "the connector did not resolve the computer's network card (resolved "
                                        + resolved + ", card " + card + ")");
                    }
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void aConnectorNoticesTheNetworkCardBeingRemoved(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.COMPUTER.get()), COMPUTER_POS);
        place(helper, player, new ItemStack(Items.CREATIVE_ENERGY.get()), POWER_POS);

        player.setXRot(90);
        place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), COMPUTER_POS.above());

        helper.startSequence()
                .thenExecuteAfter(20, () -> cardSlot(helper)
                        .insertItem(0, new ItemStack(Items.NETWORK_INTERFACE_CARD.get()), false))
                .thenExecuteAfter(80, () -> {
                    if (adjacentInterfaceOf(helper, COMPUTER_POS.above()) == null) {
                        throw new GameTestAssertException(
                                "the connector did not find the installed network card");
                    }
                })
                .thenExecute(() -> {
                    if (cardSlot(helper).extractItem(0, 1, false).isEmpty()) {
                        throw new GameTestAssertException("could not remove the network card");
                    }
                })
                .thenExecuteAfter(80, () -> {
                    if (computerNetworkInterface(helper) != null) {
                        throw new GameTestAssertException(
                                "the computer still exposes an interface after card removal");
                    }
                    if (adjacentInterfaceOf(helper, COMPUTER_POS.above()) != null) {
                        throw new GameTestAssertException(
                                "the connector is still holding the interface of a card that has been removed");
                    }
                })
                .thenSucceed();
    }


    // ------------------------------------------------------------- //

    private static ItemHandler cardSlot(final GameTestHelper helper) {
        return ((ComputerBlockEntity) helper.getBlockEntity(COMPUTER_POS)).getItemStackHandlers()
                .getItemHandler(DeviceTypes.CARD)
                .orElseThrow(() -> new GameTestAssertException("no card slot"));
    }

    @Nullable
    private static NetworkInterface computerNetworkInterface(final GameTestHelper helper) {
        return Capabilities.get(helper.getBlockEntity(COMPUTER_POS),
                Capabilities.NETWORK_INTERFACE, Direction.UP);
    }

    private static void placeLink(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        place(helper, player, new ItemStack(Items.NETWORK_HUB.get()), HUB_A);
        place(helper, player, new ItemStack(Items.NETWORK_HUB.get()), HUB_B);

        player.setXRot(90); // look down
        place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), CONNECTOR_A);
        place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), CONNECTOR_B);

        final ConnectionResult result = NetworkConnectorBlockEntity.connect(
                helper.getBlockEntity(CONNECTOR_A), helper.getBlockEntity(CONNECTOR_B));
        if (result != ConnectionResult.SUCCESS) {
            throw new GameTestAssertException("could not link the connectors: " + result);
        }
    }

    private static NetworkInterface connectorInterface(final GameTestHelper helper, final BlockPos pos) {
        final NetworkInterface networkInterface = Capabilities.get(
                helper.getBlockEntity(pos), Capabilities.NETWORK_INTERFACE, Direction.DOWN);
        if (networkInterface == null) {
            throw new GameTestAssertException("connector at " + pos + " exposes no network interface");
        }
        return networkInterface;
    }

    @Nullable
    private static Object adjacentInterfaceOf(final GameTestHelper helper, final BlockPos pos) {
        // Only used here, so let's just grab it with reflection...
        try {
            final java.lang.reflect.Field field =
                    NetworkConnectorBlockEntity.class.getDeclaredField("adjacentInterface");
            field.setAccessible(true);
            return field.get(helper.getBlockEntity(pos));
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException("could not read the connector's adjacent interface: " + e);
        }
    }

    private static int framesReceived(final NetworkHubBlockEntity hub) {
        // Only used here, so let's just grab it with reflection...
        try {
            final java.lang.reflect.Field field =
                    NetworkHubBlockEntity.class.getDeclaredField("frameCount");
            field.setAccessible(true);
            return (Integer) field.get(hub);
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException("could not read the hub's frame counter: " + e);
        }
    }

    private NetworkingTests() {
    }
}

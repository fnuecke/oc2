/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.gametest.fixture.ConnectorFixture;
import li.cil.oc2.gametest.fixture.HubFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.util.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkingTests {
    private static final BlockPos HUB_A = new BlockPos(2, WORK_Y, 2);
    private static final BlockPos HUB_B = new BlockPos(8, WORK_Y, 2);

    private static final byte[] FRAME = new byte[64];

    // --------------------------------------------------------------------- //

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void framesFromTheLocalInterfaceReachThePeer(final GameTestHelper helper) {
        final Link link = placeLink(helper);

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                final int before = link.hubB.framesReceived();

                link.connectorA.networkInterface(Direction.DOWN)
                    .writeEthernetFrame(link.hubA.blockEntity(), FRAME, 12);

                final int after = link.hubB.framesReceived();
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
        final Link link = placeLink(helper);

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                final int before = link.hubA.framesReceived();

                link.connectorA.networkInterface(Direction.DOWN)
                    .writeEthernetFrame(link.connectorB.networkInterface(Direction.DOWN), FRAME, 12);

                if (link.hubA.framesReceived() <= before) {
                    throw new GameTestAssertException(
                        "a frame arriving over the cable was not delivered to the local block");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void framesAreNotEchoedBackToTheirSource(final GameTestHelper helper) {
        final Link link = placeLink(helper);

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                final int before = link.hubA.framesReceived();

                link.connectorA.networkInterface(Direction.DOWN)
                    .writeEthernetFrame(link.hubA.blockEntity(), FRAME, 12);

                if (link.hubA.framesReceived() != before) {
                    throw new GameTestAssertException(
                        "a frame was echoed back to the interface it came from");
                }
            })
            .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 600)
    public static void expiredFramesAreDropped(final GameTestHelper helper) {
        final Link link = placeLink(helper);

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                final int before = link.hubB.framesReceived();

                link.connectorA.networkInterface(Direction.DOWN)
                    .writeEthernetFrame(link.hubA.blockEntity(), FRAME, 0);

                if (link.hubB.framesReceived() != before) {
                    throw new GameTestAssertException("a frame with no time to live was forwarded");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static Link placeLink(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final HubFixture hubA = HubFixture.place(helper, player, HUB_A);
        final HubFixture hubB = HubFixture.place(helper, player, HUB_B);

        player.setXRot(90); // look down
        final ConnectorFixture connectorA = ConnectorFixture.place(helper, player, HUB_A.above());
        final ConnectorFixture connectorB = ConnectorFixture.place(helper, player, HUB_B.above());

        connectorA.linkTo(connectorB);

        return new Link(hubA, hubB, connectorA, connectorB);
    }

    // --------------------------------------------------------------------- //

    private NetworkingTests() {
    }

    // --------------------------------------------------------------------- //

    private record Link(HubFixture hubA, HubFixture hubB,
                        ConnectorFixture connectorA, ConnectorFixture connectorB) {
    }
}

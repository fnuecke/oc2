/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.neoforge;

import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import li.cil.oc2.common.item.Items;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import static li.cil.oc2.gametest.TestSupport.*;

@GameTestHolder(MOD_ID)
@PrefixGameTestTemplate(false)
public final class NetworkConnectorTests {
    private static final BlockPos CONNECTOR_A = new BlockPos(2, WORK_Y, 2);
    private static final BlockPos CONNECTOR_B = new BlockPos(6, WORK_Y, 2);
    private static final BlockPos OBSTRUCTION = new BlockPos(4, WORK_Y, 2);
    private static final BlockPos CONNECTOR_TOO_FAR = new BlockPos(25, WORK_Y, 2);

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void connectorsLinkWithClearLineOfSight(final GameTestHelper helper) {
        placeConnectors(helper, CONNECTOR_A, CONNECTOR_B);

        final ConnectionResult result = NetworkConnectorBlockEntity.connect(
            connector(helper, CONNECTOR_A), connector(helper, CONNECTOR_B));
        if (result != ConnectionResult.SUCCESS) {
            throw new GameTestAssertException("connecting two connectors with a clear line of "
                + "sight returned " + result);
        }

        if (!connector(helper, CONNECTOR_A).getConnectedPositions()
            .contains(helper.absolutePos(CONNECTOR_B))) {
            throw new GameTestAssertException("connector A does not list B as connected");
        }
        if (!connector(helper, CONNECTOR_B).getConnectedPositions()
            .contains(helper.absolutePos(CONNECTOR_A))) {
            throw new GameTestAssertException("connector B does not list A as connected");
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void connectorsRefuseObstructedLink(final GameTestHelper helper) {
        final Player player = placeConnectors(helper, CONNECTOR_A, CONNECTOR_B);
        place(helper, player, new ItemStack(net.minecraft.world.item.Items.STONE), OBSTRUCTION);

        final ConnectionResult result = NetworkConnectorBlockEntity.connect(
            connector(helper, CONNECTOR_A), connector(helper, CONNECTOR_B));
        if (result != ConnectionResult.FAILURE_OBSTRUCTED) {
            throw new GameTestAssertException("a solid block between two connectors should block "
                + "the link, got " + result);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void connectorsRefuseLinkBeyondRange(final GameTestHelper helper) {
        placeConnectors(helper, CONNECTOR_A, CONNECTOR_TOO_FAR);

        final ConnectionResult result = NetworkConnectorBlockEntity.connect(
            connector(helper, CONNECTOR_A), connector(helper, CONNECTOR_TOO_FAR));
        if (result != ConnectionResult.FAILURE_TOO_FAR) {
            throw new GameTestAssertException("connectors further apart than the maximum cable "
                + "length should not link, got " + result);
        }

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void networkCableLinksConnectorsWhenUsed(final GameTestHelper helper) {
        placeConnectors(helper, CONNECTOR_A, CONNECTOR_B);

        final ServerPlayer player = helper.makeMockServerPlayerInLevel();
        final ItemStack cable = new ItemStack(Items.NETWORK_CABLE.get(), 2);
        useOn(helper, player, cable, CONNECTOR_A, Direction.UP);
        useOn(helper, player, cable, CONNECTOR_B, Direction.UP);

        if (!connector(helper, CONNECTOR_A).getConnectedPositions()
            .contains(helper.absolutePos(CONNECTOR_B))) {
            throw new GameTestAssertException("using a network cable on two connectors did not "
                + "link them");
        }

        helper.succeed();
    }

    ///////////////////////////////////////////////////////////////////

    private static Player placeConnectors(final GameTestHelper helper, final BlockPos... positions) {
        final Player player = fakePlayer(helper);
        for (final BlockPos pos : positions) {
            place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), pos);
        }
        return player;
    }

    private static NetworkConnectorBlockEntity connector(final GameTestHelper helper, final BlockPos pos) {
        return helper.getBlockEntity(pos);
    }

    private NetworkConnectorTests() {
    }
}

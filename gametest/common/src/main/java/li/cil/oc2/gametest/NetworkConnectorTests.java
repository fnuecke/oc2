/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.block.NetworkConnectorBlock;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.ConnectorFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import static li.cil.oc2.gametest.util.TestSupport.*;

public final class NetworkConnectorTests {
    private static final int ON_FLOOR_Y = 1;
    private static final BlockPos CONNECTOR_A = new BlockPos(2, ON_FLOOR_Y, 2);
    private static final BlockPos CONNECTOR_B = new BlockPos(6, ON_FLOOR_Y, 2);
    private static final BlockPos OBSTRUCTION = new BlockPos(4, ON_FLOOR_Y, 2);
    private static final BlockPos CONNECTOR_TOO_FAR = new BlockPos(25, ON_FLOOR_Y, 2);
    private static final BlockPos FENCE = new BlockPos(2, WORK_Y, 5);
    private static final BlockPos WALL = new BlockPos(4, WORK_Y, 5);
    private static final BlockPos MIDAIR = new BlockPos(6, WORK_Y, 5);
    private static final int RESOLVE_TICKS = 5; // Links to other connectors resolve in the server tick.

    // --------------------------------------------------------------------- //

    public static void connectorsAttachToPostsAndNothingElse(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        helper.setBlock(FENCE, Blocks.OAK_FENCE);
        helper.setBlock(WALL, Blocks.COBBLESTONE_WALL);
        helper.setBlock(MIDAIR.below(), Blocks.AIR);

        ConnectorFixture.place(helper, player, FENCE.above());
        ConnectorFixture.place(helper, player, WALL.above());

        requireCanSurvive(helper, FENCE.above(), true);
        requireCanSurvive(helper, WALL.above(), true);
        requireCanSurvive(helper, MIDAIR, false);

        helper.succeed();
    }

    public static void connectorsLinkWithClearLineOfSight(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ConnectorFixture a = ConnectorFixture.place(helper, player, CONNECTOR_A);
        final ConnectorFixture b = ConnectorFixture.place(helper, player, CONNECTOR_B);

        a.linkTo(b);

        if (!a.isConnectedTo(b)) {
            throw new GameTestAssertException(Component.literal("connector A does not list B as connected"), 0);
        }
        if (!b.isConnectedTo(a)) {
            throw new GameTestAssertException(Component.literal("connector B does not list A as connected"), 0);
        }

        helper.succeed();
    }

    public static void connectorsRefuseObstructedLink(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ConnectorFixture a = ConnectorFixture.place(helper, player, CONNECTOR_A);
        final ConnectorFixture b = ConnectorFixture.place(helper, player, CONNECTOR_B);
        place(helper, player, new ItemStack(net.minecraft.world.item.Items.STONE), OBSTRUCTION);

        final ConnectionResult result = a.tryLinkTo(b);
        if (result != ConnectionResult.FAILURE_OBSTRUCTED) {
            throw new GameTestAssertException(Component.literal("a solid block between two connectors should block "
                + "the link, got " + result), 0);
        }

        helper.succeed();
    }

    public static void connectorsRefuseLinkBeyondRange(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ConnectorFixture a = ConnectorFixture.place(helper, player, CONNECTOR_A);
        final ConnectorFixture far = ConnectorFixture.place(helper, player, CONNECTOR_TOO_FAR);

        final ConnectionResult result = a.tryLinkTo(far);
        if (result != ConnectionResult.FAILURE_TOO_FAR) {
            throw new GameTestAssertException(Component.literal("connectors further apart than the maximum cable "
                + "length should not link, got " + result), 0);
        }

        helper.succeed();
    }

    public static void removingAConnectorUnlinksItsPeer(final GameTestHelper helper) {
        final ConnectorFixture[] linked = placeLinked(helper);
        helper.startSequence()
            .thenIdle(RESOLVE_TICKS)
            .thenExecute(() -> {
                helper.setBlock(CONNECTOR_B, Blocks.AIR);
                requireLinked(linked[0], linked[1], false, "after removing the peer");
            })
            .thenSucceed();
    }

    public static void silentlyRemovedConnectorKeepsItsPeer(final GameTestHelper helper) {
        final ConnectorFixture[] linked = placeLinked(helper);
        helper.startSequence()
            .thenIdle(RESOLVE_TICKS)
            .thenExecute(() -> {
                // Mods moving blocks, such as Sable, skip block entity side effects.
                helper.getLevel().setBlock(helper.absolutePos(CONNECTOR_B), Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                requireLinked(linked[0], linked[1], true, "after silently removing the peer");
            })
            .thenSucceed();
    }

    public static void stateChangeKeepsTheLink(final GameTestHelper helper) {
        final ConnectorFixture[] linked = placeLinked(helper);
        helper.startSequence()
            .thenIdle(RESOLVE_TICKS)
            .thenExecute(() -> {
                final BlockState state = helper.getBlockState(CONNECTOR_B);
                helper.setBlock(CONNECTOR_B, state.setValue(FaceAttachedHorizontalDirectionalBlock.FACING,
                    state.getValue(FaceAttachedHorizontalDirectionalBlock.FACING).getClockWise()));
                requireLinked(linked[0], linked[1], true, "after rotating the peer");
                requireLinked(linked[1], linked[0], true, "after rotating it");
            })
            .thenSucceed();
    }

    @SuppressWarnings("removal")
    public static void networkCableLinksConnectorsWhenUsed(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ConnectorFixture a = ConnectorFixture.place(helper, player, CONNECTOR_A);
        final ConnectorFixture b = ConnectorFixture.place(helper, player, CONNECTOR_B);

        final ServerPlayer cablePlayer = helper.makeMockServerPlayerInLevel();
        final ItemStack cable = new ItemStack(Items.NETWORK_CABLE.get(), 2);
        useOn(helper, cablePlayer, cable, CONNECTOR_A, Direction.UP);
        useOn(helper, cablePlayer, cable, CONNECTOR_B, Direction.UP);

        if (!a.isConnectedTo(b)) {
            throw new GameTestAssertException(Component.literal("network cable did not link the connectors"), 0);
        }

        helper.succeed();
    }

    public static void connectorResolvesNetworkCard(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.placePowered(helper, player);

        player.setXRot(90);
        final ConnectorFixture connector = ConnectorFixture.place(helper, player, computer.pos().above());

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                if (computer.networkInterface(Direction.UP) != null) {
                    throw new GameTestAssertException(Component.literal(
                        "a computer with no network card must not expose a network interface"), 0);
                }

                computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.NETWORK_INTERFACE_CARD.get()));
            })
            .thenExecuteAfter(80, () -> {
                final NetworkInterface card = computer.networkInterface(Direction.UP);
                if (card == null) {
                    throw new GameTestAssertException(Component.literal(
                        "an installed network card is not reachable as a block capability"), 0);
                }

                final Object resolved = connector.adjacentInterface();
                if (resolved != card) {
                    throw new GameTestAssertException(Component.literal(
                        "the connector did not resolve the computer's network card (resolved "
                            + resolved + ", card " + card + ")"), 0);
                }
            })
            .thenSucceed();
    }

    public static void connectorDropsRemovedCard(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.placePowered(helper, player);

        player.setXRot(90);
        final ConnectorFixture connector = ConnectorFixture.place(helper, player, computer.pos().above());

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer.install(DeviceTypes.CARD.get(),
                new ItemStack(Items.NETWORK_INTERFACE_CARD.get())))
            .thenExecuteAfter(80, () -> {
                if (connector.adjacentInterface() == null) {
                    throw new GameTestAssertException(Component.literal("connector did not find the network card"), 0);
                }
                if (!helper.getBlockState(connector.pos()).getValue(NetworkConnectorBlock.CONNECTED)) {
                    throw new GameTestAssertException(Component.literal("a connector with an adjacent interface must be in the connected state"), 0);
                }
            })
            .thenExecute(() -> computer.uninstall(DeviceTypes.CARD.get()))
            .thenExecuteAfter(80, () -> {
                if (computer.networkInterface(Direction.UP) != null) {
                    throw new GameTestAssertException(Component.literal(
                        "the computer still exposes an interface after card removal"), 0);
                }
                if (connector.adjacentInterface() != null) {
                    throw new GameTestAssertException(Component.literal(
                        "the connector is still holding the interface of a card that has been removed"), 0);
                }
                if (helper.getBlockState(connector.pos()).getValue(NetworkConnectorBlock.CONNECTED)) {
                    throw new GameTestAssertException(Component.literal("a connector without an adjacent interface must not be in the connected state"), 0);
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static ConnectorFixture[] placeLinked(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ConnectorFixture a = ConnectorFixture.place(helper, player, CONNECTOR_A);
        final ConnectorFixture b = ConnectorFixture.place(helper, player, CONNECTOR_B);
        a.linkTo(b);
        requireLinked(a, b, true, "after linking");
        return new ConnectorFixture[]{a, b};
    }

    private static void requireLinked(final ConnectorFixture a, final ConnectorFixture b, final boolean expected, final String when) {
        if (a.isConnectedTo(b) != expected) {
            throw new GameTestAssertException(Component.literal("connector at " + a.pos() + (expected ? " lost" : " kept")
                + " its link to " + b.pos() + " " + when), 0);
        }
    }

    private static void requireCanSurvive(final GameTestHelper helper, final BlockPos pos, final boolean expected) {
        final BlockState state = li.cil.oc2.common.block.Blocks.NETWORK_CONNECTOR.get().defaultBlockState()
            .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
            .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
        if (state.canSurvive(helper.getLevel(), helper.absolutePos(pos)) != expected) {
            throw new GameTestAssertException(Component.literal("a connector standing on [" + helper.getBlockState(pos.below())
                + "] should " + (expected ? "" : "not ") + "survive"), 0);
        }
    }

    // --------------------------------------------------------------------- //

    private NetworkConnectorTests() {
    }
}

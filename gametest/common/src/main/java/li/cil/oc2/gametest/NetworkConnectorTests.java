/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.api.bus.device.DeviceTypes;
import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.fixture.ComputerFixture;
import li.cil.oc2.gametest.fixture.ConnectorFixture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;

import static li.cil.oc2.gametest.util.TestSupport.*;

public final class NetworkConnectorTests {
    private static final BlockPos CONNECTOR_A = new BlockPos(2, WORK_Y, 2);
    private static final BlockPos CONNECTOR_B = new BlockPos(6, WORK_Y, 2);
    private static final BlockPos OBSTRUCTION = new BlockPos(4, WORK_Y, 2);
    private static final BlockPos CONNECTOR_TOO_FAR = new BlockPos(25, WORK_Y, 2);
    private static final BlockPos FENCE = new BlockPos(2, WORK_Y, 5);
    private static final BlockPos WALL = new BlockPos(4, WORK_Y, 5);
    private static final BlockPos MIDAIR = new BlockPos(6, WORK_Y, 5);

    // --------------------------------------------------------------------- //

    public static void connectorsAttachToPostsAndNothingElse(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        helper.setBlock(FENCE, Blocks.OAK_FENCE);
        helper.setBlock(WALL, Blocks.COBBLESTONE_WALL);
        helper.setBlock(MIDAIR.below(), Blocks.AIR);

        // Neither has a full face, but both have a post covering the center of it.
        ConnectorFixture.place(helper, player, FENCE.above());
        ConnectorFixture.place(helper, player, WALL.above());

        // Still nothing to hold on to where there is no block at all.
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
            throw new GameTestAssertException("connector A does not list B as connected");
        }
        if (!b.isConnectedTo(a)) {
            throw new GameTestAssertException("connector B does not list A as connected");
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
            throw new GameTestAssertException("a solid block between two connectors should block "
                + "the link, got " + result);
        }

        helper.succeed();
    }

    public static void connectorsRefuseLinkBeyondRange(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ConnectorFixture a = ConnectorFixture.place(helper, player, CONNECTOR_A);
        final ConnectorFixture far = ConnectorFixture.place(helper, player, CONNECTOR_TOO_FAR);

        final ConnectionResult result = a.tryLinkTo(far);
        if (result != ConnectionResult.FAILURE_TOO_FAR) {
            throw new GameTestAssertException("connectors further apart than the maximum cable "
                + "length should not link, got " + result);
        }

        helper.succeed();
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
            throw new GameTestAssertException("using a network cable on two connectors did not "
                + "link them");
        }

        helper.succeed();
    }

    public static void aConnectorResolvesTheNetworkCardOfTheComputerItIsOn(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        player.setXRot(90);
        final ConnectorFixture connector = ConnectorFixture.place(helper, player, computer.pos().above());

        helper.startSequence()
            .thenExecuteAfter(40, () -> {
                if (computer.networkInterface(Direction.UP) != null) {
                    throw new GameTestAssertException(
                        "a computer with no network card must not expose a network interface");
                }

                computer.install(DeviceTypes.CARD.get(), new ItemStack(Items.NETWORK_INTERFACE_CARD.get()));
            })
            .thenExecuteAfter(80, () -> {
                final NetworkInterface card = computer.networkInterface(Direction.UP);
                if (card == null) {
                    throw new GameTestAssertException(
                        "an installed network card is not reachable as a block capability");
                }

                final Object resolved = connector.adjacentInterface();
                if (resolved != card) {
                    throw new GameTestAssertException(
                        "the connector did not resolve the computer's network card (resolved "
                            + resolved + ", card " + card + ")");
                }
            })
            .thenSucceed();
    }

    public static void aConnectorNoticesTheNetworkCardBeingRemoved(final GameTestHelper helper) {
        final Player player = fakePlayer(helper);
        final ComputerFixture computer = ComputerFixture.place(helper, player);
        placePower(helper, player);

        player.setXRot(90);
        final ConnectorFixture connector = ConnectorFixture.place(helper, player, computer.pos().above());

        helper.startSequence()
            .thenExecuteAfter(20, () -> computer.install(DeviceTypes.CARD.get(),
                new ItemStack(Items.NETWORK_INTERFACE_CARD.get())))
            .thenExecuteAfter(80, () -> {
                if (connector.adjacentInterface() == null) {
                    throw new GameTestAssertException(
                        "the connector did not find the installed network card");
                }
            })
            .thenExecute(() -> computer.uninstall(DeviceTypes.CARD.get()))
            .thenExecuteAfter(80, () -> {
                if (computer.networkInterface(Direction.UP) != null) {
                    throw new GameTestAssertException(
                        "the computer still exposes an interface after card removal");
                }
                if (connector.adjacentInterface() != null) {
                    throw new GameTestAssertException(
                        "the connector is still holding the interface of a card that has been removed");
                }
            })
            .thenSucceed();
    }

    // --------------------------------------------------------------------- //

    private static void requireCanSurvive(final GameTestHelper helper, final BlockPos pos, final boolean expected) {
        final BlockState state = li.cil.oc2.common.block.Blocks.NETWORK_CONNECTOR.get().defaultBlockState()
            .setValue(FaceAttachedHorizontalDirectionalBlock.FACE, AttachFace.FLOOR)
            .setValue(FaceAttachedHorizontalDirectionalBlock.FACING, Direction.NORTH);
        if (state.canSurvive(helper.getLevel(), helper.absolutePos(pos)) != expected) {
            throw new GameTestAssertException("a connector standing on [" + helper.getBlockState(pos.below())
                + "] should " + (expected ? "" : "not ") + "survive");
        }
    }

    private NetworkConnectorTests() {
    }
}

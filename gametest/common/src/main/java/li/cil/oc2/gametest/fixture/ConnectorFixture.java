/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest.fixture;

import li.cil.oc2.api.capabilities.NetworkInterface;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity.ConnectionResult;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.gametest.util.TestSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.lang.reflect.Field;

import static li.cil.oc2.gametest.util.TestSupport.fakePlayer;

public final class ConnectorFixture {
    private final GameTestHelper helper;
    private final BlockPos pos;

    // --------------------------------------------------------------------- //

    public static ConnectorFixture place(final GameTestHelper helper, final BlockPos pos) {
        return place(helper, fakePlayer(helper), pos);
    }

    public static ConnectorFixture place(final GameTestHelper helper, final Player player, final BlockPos pos) {
        TestSupport.place(helper, player, new ItemStack(Items.NETWORK_CONNECTOR.get()), pos);
        return new ConnectorFixture(helper, pos);
    }

    // --------------------------------------------------------------------- //

    public BlockPos pos() {
        return pos;
    }

    public NetworkConnectorBlockEntity blockEntity() {
        return helper.getBlockEntity(pos);
    }

    public ConnectionResult tryLinkTo(final ConnectorFixture other) {
        return NetworkConnectorBlockEntity.connect(blockEntity(), other.blockEntity());
    }

    public void linkTo(final ConnectorFixture other) {
        final ConnectionResult result = tryLinkTo(other);
        if (result != ConnectionResult.SUCCESS) {
            throw new GameTestAssertException("could not link the connectors: " + result);
        }
    }

    public boolean isConnectedTo(final ConnectorFixture other) {
        return blockEntity().getConnectedPositions().contains(helper.absolutePos(other.pos()));
    }

    public NetworkInterface networkInterface(final Direction side) {
        final NetworkInterface networkInterface = Capabilities.get(
            blockEntity(), Capabilities.NETWORK_INTERFACE, side);
        if (networkInterface == null) {
            throw new GameTestAssertException("connector at " + pos + " exposes no network interface");
        }
        return networkInterface;
    }

    /**
     * Private state, but the only place the "did it notice the card" question can be answered.
     */
    @Nullable
    public Object adjacentInterface() {
        try {
            final Field field = NetworkConnectorBlockEntity.class.getDeclaredField("adjacentInterface");
            field.setAccessible(true);
            final Invalidatable<?> adjacent = (Invalidatable<?>) field.get(blockEntity());
            return adjacent != null && adjacent.isPresent() ? adjacent.get() : null;
        } catch (final ReflectiveOperationException e) {
            throw new GameTestAssertException("could not read the connector's adjacent interface: " + e);
        }
    }

    // --------------------------------------------------------------------- //

    private ConnectorFixture(final GameTestHelper helper, final BlockPos pos) {
        this.helper = helper;
        this.pos = pos;
    }
}

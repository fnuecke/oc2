/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public abstract class InternetGatewayConfigurationMessage extends AbstractMessage {
    public static final int MAX_PEER_LENGTH = 45;

    protected BlockPos pos;
    protected String peer;
    protected int port;
    protected int vni;
    protected InternetGatewayBlockEntity.InternetMode mode;

    protected InternetGatewayConfigurationMessage(
        final InternetGatewayBlockEntity gateway,
        final String peer,
        final int port,
        final int vni,
        final InternetGatewayBlockEntity.InternetMode mode
    ) {
        this.pos = gateway.getBlockPos();
        this.peer = peer;
        this.port = port;
        this.vni = vni;
        this.mode = mode;
    }

    protected InternetGatewayConfigurationMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        peer = buffer.readUtf(MAX_PEER_LENGTH);
        port = buffer.readVarInt();
        vni = buffer.readVarInt();
        mode = InternetGatewayBlockEntity.InternetMode.valueOf(buffer.readUtf());
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeUtf(peer, MAX_PEER_LENGTH);
        buffer.writeVarInt(port);
        buffer.writeVarInt(vni);
        buffer.writeUtf(mode.name());
    }

    public static final class ToServer extends InternetGatewayConfigurationMessage {
        public ToServer(final InternetGatewayBlockEntity gateway, final String peer, final int port,
                        final int vni, final InternetGatewayBlockEntity.InternetMode mode) {
            super(gateway, peer, port, vni, mode);
        }

        public ToServer(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos,
                InternetGatewayBlockEntity.class,
                (player, gateway) -> gateway.setConfiguration(peer, port, vni, mode));
        }
    }

    public static final class ToClient extends InternetGatewayConfigurationMessage {
        public ToClient(final InternetGatewayBlockEntity gateway, final String peer, final int port,
                        final int vni, final InternetGatewayBlockEntity.InternetMode mode) {
            super(gateway, peer, port, vni, mode);
        }

        public ToClient(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withClientBlockEntityAt(pos, InternetGatewayBlockEntity.class,
                gateway -> gateway.setConfigurationClient(peer, port, vni, mode));
        }
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public abstract class InternetGatewayConfigurationMessage extends AbstractMessage {
    public static final int MAX_PEER_LENGTH = 45;

    protected BlockPos pos;
    protected InternetGatewayBlockEntity.Settings settings;

    protected InternetGatewayConfigurationMessage(final InternetGatewayBlockEntity gateway, final InternetGatewayBlockEntity.Settings settings) {
        this.pos = gateway.getBlockPos();
        this.settings = settings;
    }

    protected InternetGatewayConfigurationMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        settings = new InternetGatewayBlockEntity.Settings(
            buffer.readEnum(InternetGatewayBlockEntity.InternetMode.class),
            buffer.readInt(),
            buffer.readUtf(MAX_PEER_LENGTH),
            buffer.readVarInt(),
            buffer.readVarInt()
        );
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(settings.mode());
        buffer.writeInt(settings.gatewayAddress());
        buffer.writeUtf(settings.vxlanPeer(), MAX_PEER_LENGTH);
        buffer.writeVarInt(settings.vxlanPort());
        buffer.writeVarInt(settings.vxlanVni());
    }

    public static final class ToServer extends InternetGatewayConfigurationMessage {
        public ToServer(final InternetGatewayBlockEntity gateway, final InternetGatewayBlockEntity.Settings settings) {
            super(gateway, settings);
        }

        public ToServer(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            if (settings.mode() == InternetGatewayBlockEntity.InternetMode.VXLAN && !Config.vxlanEnabled) {
                return;
            }
            MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos,
                InternetGatewayBlockEntity.class,
                (player, gateway) -> gateway.setConfiguration(settings));
        }
    }

    public static final class ToClient extends InternetGatewayConfigurationMessage {
        public ToClient(final InternetGatewayBlockEntity gateway, final InternetGatewayBlockEntity.Settings settings) {
            super(gateway, settings);
        }

        public ToClient(final RegistryFriendlyByteBuf buffer) {
            super(buffer);
        }

        @Override
        protected void handleMessage(final NetworkManager.PacketContext context) {
            MessageUtils.withClientBlockEntityAt(pos, InternetGatewayBlockEntity.class,
                gateway -> gateway.setConfigurationClient(settings));
        }
    }
}

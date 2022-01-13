package li.cil.oc2.common.network.message;

import li.cil.oc2.common.container.NetworkTunnelContainer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkEvent;

public final class NetworkTunnelLinkMessage extends AbstractMessage {
    private int containerId;

    ///////////////////////////////////////////////////////////////////

    public NetworkTunnelLinkMessage(final int containerId) {
        this.containerId = containerId;
    }

    public NetworkTunnelLinkMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        containerId = buffer.readVarInt();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        final ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }

        final AbstractContainerMenu container = player.containerMenu;
        if (container.containerId != containerId) {
            return;
        }

        if (container instanceof NetworkTunnelContainer networkTunnelContainer) {
            networkTunnelContainer.createTunnel();
        }
    }
}

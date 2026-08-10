/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.container.NetworkTunnelContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class NetworkTunnelLinkMessage extends AbstractMessage {
    private int containerId;

    ///////////////////////////////////////////////////////////////////

    public NetworkTunnelLinkMessage(final int containerId) {
        this.containerId = containerId;
    }

    public NetworkTunnelLinkMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        containerId = buffer.readVarInt();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        if (!(context.getPlayer() instanceof final ServerPlayer player)) {
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

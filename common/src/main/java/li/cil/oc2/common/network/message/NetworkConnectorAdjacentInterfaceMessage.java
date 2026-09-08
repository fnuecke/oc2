/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class NetworkConnectorAdjacentInterfaceMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean hasAdjacentInterface;

    // --------------------------------------------------------------------- //

    public NetworkConnectorAdjacentInterfaceMessage(final NetworkConnectorBlockEntity networkConnector) {
        this.pos = networkConnector.getBlockPos();
        this.hasAdjacentInterface = networkConnector.hasAdjacentInterface();
    }

    public NetworkConnectorAdjacentInterfaceMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        hasAdjacentInterface = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(hasAdjacentInterface);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, NetworkConnectorBlockEntity.class,
            networkConnector -> networkConnector.setHasAdjacentInterfaceClient(hasAdjacentInterface));
    }
}

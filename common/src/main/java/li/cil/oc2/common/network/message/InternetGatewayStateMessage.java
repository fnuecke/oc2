/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.InternetGatewayBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class InternetGatewayStateMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean isOperational;

    // --------------------------------------------------------------------- //

    public InternetGatewayStateMessage(final InternetGatewayBlockEntity gateway, final boolean isOperational) {
        this.pos = gateway.getBlockPos();
        this.isOperational = isOperational;
    }

    public InternetGatewayStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        isOperational = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(isOperational);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, InternetGatewayBlockEntity.class,
            gateway -> gateway.setOperationalClient(isOperational));
    }
}

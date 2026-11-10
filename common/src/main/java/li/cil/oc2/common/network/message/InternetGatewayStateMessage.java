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
    private long energy;

    // --------------------------------------------------------------------- //

    public InternetGatewayStateMessage(final InternetGatewayBlockEntity gateway, final boolean isOperational, final long energy) {
        this.pos = gateway.getBlockPos();
        this.isOperational = isOperational;
        this.energy = energy;
    }

    public InternetGatewayStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        isOperational = buffer.readBoolean();
        energy = buffer.readVarLong();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(isOperational);
        buffer.writeVarLong(energy);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, InternetGatewayBlockEntity.class,
            gateway -> gateway.setStateClient(isOperational, energy));
    }
}

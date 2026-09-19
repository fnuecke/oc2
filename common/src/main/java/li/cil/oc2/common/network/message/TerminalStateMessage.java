/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class TerminalStateMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean isConnected;

    // --------------------------------------------------------------------- //

    public TerminalStateMessage(final TerminalBlockEntity terminal, final boolean isConnected) {
        this.pos = terminal.getBlockPos();
        this.isConnected = isConnected;
    }

    public TerminalStateMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        isConnected = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(isConnected);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, TerminalBlockEntity.class,
            terminal -> terminal.setConnectedClient(isConnected));
    }
}

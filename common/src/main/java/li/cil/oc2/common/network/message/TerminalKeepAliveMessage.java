/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class TerminalKeepAliveMessage extends AbstractMessage {
    private BlockPos pos;

    // --------------------------------------------------------------------- //

    public TerminalKeepAliveMessage(final TerminalBlockEntity terminal) {
        this.pos = terminal.getBlockPos();
    }

    public TerminalKeepAliveMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, TerminalBlockEntity.class,
            (player, terminal) -> terminal.handleUsedBy(player));
    }
}

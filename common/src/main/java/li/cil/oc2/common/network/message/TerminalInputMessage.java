/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.nio.ByteBuffer;

public final class TerminalInputMessage extends AbstractTerminalBlockMessage {
    public TerminalInputMessage(final TerminalBlockEntity terminal, final ByteBuffer data) {
        super(terminal.getBlockPos(), data);
    }

    public TerminalInputMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerBlockEntityForInteraction(context, pos, TerminalBlockEntity.class,
            (player, terminal) -> terminal.getTerminal().putInput(ByteBuffer.wrap(data)));
    }
}

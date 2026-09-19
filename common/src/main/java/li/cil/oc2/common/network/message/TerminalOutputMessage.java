/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.nio.ByteBuffer;

public final class TerminalOutputMessage extends AbstractTerminalBlockMessage {
    public TerminalOutputMessage(final TerminalBlockEntity terminal, final ByteBuffer data) {
        super(terminal.getBlockPos(), data);
    }

    public TerminalOutputMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, TerminalBlockEntity.class,
            terminal -> terminal.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}

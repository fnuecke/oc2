/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.nio.ByteBuffer;

public final class TerminalOutputMessage extends AbstractTerminalBlockMessage {
    private boolean hasFrameError;

    // --------------------------------------------------------------------- //

    public TerminalOutputMessage(final TerminalBlockEntity terminal, final ByteBuffer data, final boolean hasFrameError) {
        super(terminal.getBlockPos(), data);
        this.hasFrameError = hasFrameError;
    }

    public TerminalOutputMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        super.fromBytes(buffer);
        hasFrameError = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        super.toBytes(buffer);
        buffer.writeBoolean(hasFrameError);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, TerminalBlockEntity.class, terminal -> {
            terminal.getTerminal().putOutput(ByteBuffer.wrap(data));
            if (hasFrameError) {
                terminal.handleFrameErrorClient();
            }
        });
    }
}

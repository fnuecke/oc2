/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.nio.ByteBuffer;

public final class ComputerTerminalOutputMessage extends AbstractTerminalBlockMessage {
    public ComputerTerminalOutputMessage(final ComputerBlockEntity computer, final ByteBuffer data) {
        super(computer, data);
    }

    public ComputerTerminalOutputMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ComputerBlockEntity.class,
            computer -> computer.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}

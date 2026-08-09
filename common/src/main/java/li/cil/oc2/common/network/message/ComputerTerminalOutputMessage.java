/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ComputerBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;

public final class ComputerTerminalOutputMessage extends AbstractTerminalBlockMessage {
    public ComputerTerminalOutputMessage(final ComputerBlockEntity computer, final ByteBuffer data) {
        super(computer, data);
    }

    public ComputerTerminalOutputMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientBlockEntityAt(pos, ComputerBlockEntity.class,
            computer -> computer.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}

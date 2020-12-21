package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.PacketByteBuf;

import java.nio.ByteBuffer;

public final class TerminalBlockOutputMessage extends AbstractTerminalBlockMessage {
    public TerminalBlockOutputMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public TerminalBlockOutputMessage(final PacketByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static void handleMessage(final AbstractTerminalBlockMessage message, final Network.MessageContext context) {
        context.enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                tileEntity -> tileEntity.getTerminal().putOutput(ByteBuffer.wrap(message.data))));
    }
}

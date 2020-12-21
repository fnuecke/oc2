package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.PacketByteBuf;

import java.nio.ByteBuffer;

public final class TerminalBlockInputMessage extends AbstractTerminalBlockMessage {
    public TerminalBlockInputMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public TerminalBlockInputMessage(final PacketByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static void handleMessage(final AbstractTerminalBlockMessage message, final Network.MessageContext context) {
        context.enqueueWork(() -> MessageUtils.withServerTileEntityAt(context, message.pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.getTerminal().putInput(ByteBuffer.wrap(message.data))));
    }
}

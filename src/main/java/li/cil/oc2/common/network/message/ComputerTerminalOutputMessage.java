package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;

public final class ComputerTerminalOutputMessage extends AbstractTerminalBlockMessage {
    public ComputerTerminalOutputMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        super(tileEntity, data);
    }

    public ComputerTerminalOutputMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, ComputerTileEntity.class,
                tileEntity -> tileEntity.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}

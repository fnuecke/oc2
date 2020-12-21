package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalBlockMessage {
    protected BlockPos pos;
    protected byte[] data;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalBlockMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        this.pos = tileEntity.getPos();
        this.data = data.array();
    }

    protected AbstractTerminalBlockMessage(final PacketByteBuf buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public void fromBytes(final PacketByteBuf buffer) {
        pos = buffer.readBlockPos();
        data = buffer.readByteArray();
    }

    public static void toBytes(final AbstractTerminalBlockMessage message, final PacketByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByteArray(message.data);
    }
}

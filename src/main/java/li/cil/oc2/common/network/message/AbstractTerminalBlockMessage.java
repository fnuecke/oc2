package li.cil.oc2.common.network.message;

import li.cil.oc2.common.tile.ComputerTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalBlockMessage {
    protected BlockPos pos;
    protected byte[] data;

    protected AbstractTerminalBlockMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        this.pos = tileEntity.getPos();
        this.data = data.array();
    }

    protected AbstractTerminalBlockMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        data = buffer.readByteArray();
    }

    public static void toBytes(final AbstractTerminalBlockMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeByteArray(message.data);
    }
}

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalBlockMessage extends AbstractMessage {
    protected BlockPos pos;
    protected byte[] data;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalBlockMessage(final ComputerTileEntity tileEntity, final ByteBuffer data) {
        this.pos = tileEntity.getBlockPos();
        this.data = data.array();
    }

    protected AbstractTerminalBlockMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByteArray(data);
    }
}

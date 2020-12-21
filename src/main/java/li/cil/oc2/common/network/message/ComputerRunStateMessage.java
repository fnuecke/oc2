package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class ComputerRunStateMessage {
    private BlockPos pos;
    private ComputerTileEntity.RunState runState;

    ///////////////////////////////////////////////////////////////////

    public ComputerRunStateMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getPos();
        this.runState = tileEntity.getRunState();
    }

    public ComputerRunStateMessage(final PacketByteBuf buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static void handleMessage(final ComputerRunStateMessage message, final Network.MessageContext context) {
        context.enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.setRunStateClient(message.runState)));
    }

    public void fromBytes(final PacketByteBuf buffer) {
        pos = buffer.readBlockPos();
        runState = buffer.readEnumConstant(ComputerTileEntity.RunState.class);
    }

    public static void toBytes(final ComputerRunStateMessage message, final PacketByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnumConstant(message.runState);
    }
}

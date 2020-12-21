package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public class ComputerBusStateMessage {
    private BlockPos pos;
    private AbstractDeviceBusController.BusState busState;

    ///////////////////////////////////////////////////////////////////

    public ComputerBusStateMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getPos();
        this.busState = tileEntity.getBusState();
    }

    public ComputerBusStateMessage(final PacketByteBuf buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static void handleMessage(final ComputerBusStateMessage message, final Network.MessageContext context) {
        context.enqueueWork(() -> MessageUtils.withClientTileEntityAt(message.pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.setBusStateClient(message.busState)));
    }

    public void fromBytes(final PacketByteBuf buffer) {
        pos = buffer.readBlockPos();
        busState = buffer.readEnumConstant(AbstractDeviceBusController.BusState.class);
    }

    public static void toBytes(final ComputerBusStateMessage message, final PacketByteBuf buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeEnumConstant(message.busState);
    }
}

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

public final class ComputerBusStateMessage extends AbstractMessage {
    private BlockPos pos;
    private CommonDeviceBusController.BusState value;

    ///////////////////////////////////////////////////////////////////

    public ComputerBusStateMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getBlockPos();
        this.value = tileEntity.getVirtualMachine().getBusState();
    }

    public ComputerBusStateMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        value = buffer.readEnum(CommonDeviceBusController.BusState.class);
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeEnum(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.getVirtualMachine().setBusStateClient(value));
    }
}

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

public final class ComputerRunStateMessage extends AbstractMessage {
    private BlockPos pos;
    private VMRunState value;

    ///////////////////////////////////////////////////////////////////

    public ComputerRunStateMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getBlockPos();
        this.value = tileEntity.getVirtualMachine().getRunState();
    }

    public ComputerRunStateMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        value = buffer.readEnum(VMRunState.class);
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
                (tileEntity) -> tileEntity.getVirtualMachine().setRunStateClient(value));
    }
}

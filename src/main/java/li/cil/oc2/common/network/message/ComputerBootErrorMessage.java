package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

public final class ComputerBootErrorMessage extends AbstractMessage {
    private BlockPos pos;
    private ITextComponent value;

    ///////////////////////////////////////////////////////////////////

    public ComputerBootErrorMessage(final ComputerTileEntity tileEntity) {
        this.pos = tileEntity.getBlockPos();
        this.value = tileEntity.getVirtualMachine().getBootError();
    }

    public ComputerBootErrorMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        value = buffer.readComponent();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeComponent(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientTileEntityAt(pos, ComputerTileEntity.class,
                (tileEntity) -> tileEntity.getVirtualMachine().setBootErrorClient(value));
    }
}

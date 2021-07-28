package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public final class OpenComputerInventoryMessage extends AbstractMessage {
    private BlockPos pos;

    ///////////////////////////////////////////////////////////////////

    public OpenComputerInventoryMessage(final ComputerTileEntity computer) {
        this.pos = computer.getBlockPos();
    }

    public OpenComputerInventoryMessage(final PacketBuffer buffer) {
        super(buffer);
    }
    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeBlockPos(pos);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerTileEntityAt(context, pos, ComputerTileEntity.class,
                (computer) -> computer.openInventoryScreen(context.getSender()));
    }
}

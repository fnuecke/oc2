package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

public final class OpenComputerTerminalMessage extends AbstractMessage {
    private BlockPos pos;

    ///////////////////////////////////////////////////////////////////

    public OpenComputerTerminalMessage(final ComputerTileEntity computer) {
        this.pos = computer.getBlockPos();
    }

    public OpenComputerTerminalMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerTileEntityAt(context, pos, ComputerTileEntity.class,
                (computer) -> computer.openTerminalScreen(context.getSender()));
    }
}

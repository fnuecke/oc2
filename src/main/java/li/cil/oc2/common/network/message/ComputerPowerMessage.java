package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraftforge.network.NetworkEvent;

public final class ComputerPowerMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean power;

    ///////////////////////////////////////////////////////////////////

    public ComputerPowerMessage(final ComputerTileEntity computer, final boolean power) {
        this.pos = computer.getBlockPos();
        this.power = power;
    }

    public ComputerPowerMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        power = buffer.readBoolean();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(power);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerTileEntityAt(context, pos, ComputerTileEntity.class,
                (computer) -> {
                    if (power) {
                        computer.start();
                    } else {
                        computer.stop();
                    }
                });
    }
}

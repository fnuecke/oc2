package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerTileEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

public final class ComputerPowerMessage extends AbstractMessage {
    private BlockPos pos;
    private boolean power;

    ///////////////////////////////////////////////////////////////////

    public ComputerPowerMessage(final ComputerTileEntity computer, final boolean power) {
        this.pos = computer.getBlockPos();
        this.power = power;
    }

    public ComputerPowerMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        power = buffer.readBoolean();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
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

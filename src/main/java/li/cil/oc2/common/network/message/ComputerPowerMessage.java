package li.cil.oc2.common.network.message;

import li.cil.oc2.common.block.entity.ComputerTileEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ComputerPowerMessage {
    private BlockPos pos;
    private boolean power;

    ///////////////////////////////////////////////////////////////////

    public ComputerPowerMessage(final ComputerTileEntity computer, final boolean power) {
        this.pos = computer.getPos();
        this.power = power;
    }

    public ComputerPowerMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ComputerPowerMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerTileEntityAt(context, message.pos, ComputerTileEntity.class,
                (computer) -> {
                    final ServerPlayerEntity player = context.get().getSender();
                    if (player != null && computer.getPos().withinDistance(player.getPositionVec(), 8)) {
                        if (message.power) {
                            computer.start();
                        } else {
                            computer.stop();
                        }
                    }
                }));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        pos = buffer.readBlockPos();
        power = buffer.readBoolean();
    }

    public static void toBytes(final ComputerPowerMessage message, final PacketBuffer buffer) {
        buffer.writeBlockPos(message.pos);
        buffer.writeBoolean(message.power);
    }
}

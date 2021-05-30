package li.cil.oc2.common.network.message;

import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.tileentity.ComputerBlockEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class ComputerPowerMessage {
    private BlockPos pos;
    private boolean power;

    ///////////////////////////////////////////////////////////////////

    public ComputerPowerMessage(final ComputerBlockEntity computer, final boolean power) {
        this.pos = computer.getBlockPos();
        this.power = power;
    }

    public ComputerPowerMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final ComputerPowerMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerBlockEntityAt(context, message.pos, ComputerBlockEntity.class,
                (computer) -> {
                    final ServerPlayerEntity player = context.get().getSender();
                    if (player != null && computer.getBlockPos().closerThan(player.position(), 8)) {
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

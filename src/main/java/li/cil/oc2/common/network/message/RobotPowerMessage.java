package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class RobotPowerMessage {
    private int entityId;
    private boolean power;

    ///////////////////////////////////////////////////////////////////

    public RobotPowerMessage(final RobotEntity robot, final boolean power) {
        this.entityId = robot.getId();
        this.power = power;
    }

    public RobotPowerMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RobotPowerMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerEntity(context, message.entityId, RobotEntity.class,
                (robot) -> {
                    final ServerPlayerEntity player = context.get().getSender();
                    if (player != null && robot.closerThan(player, 8)) {
                        if (message.power) {
                            robot.start();
                        } else {
                            robot.stop();
                        }
                    }
                }));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        power = buffer.readBoolean();
    }

    public static void toBytes(final RobotPowerMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeBoolean(message.power);
    }
}

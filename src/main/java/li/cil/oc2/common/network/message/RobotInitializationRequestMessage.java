package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class RobotInitializationRequestMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public RobotInitializationRequestMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
    }

    public RobotInitializationRequestMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RobotInitializationRequestMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerEntity(context, message.entityId, RobotEntity.class,
                (robot) -> Network.INSTANCE.reply(new RobotInitializationMessage(robot), context.get())));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
    }

    public static void toBytes(final RobotInitializationRequestMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
    }
}

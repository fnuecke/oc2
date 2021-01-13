package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.AbstractDeviceBusController;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class RobotBusStateMessage {
    private int entityId;
    private AbstractDeviceBusController.BusState value;

    ///////////////////////////////////////////////////////////////////

    public RobotBusStateMessage(final RobotEntity robot) {
        this.entityId = robot.getEntityId();
        this.value = robot.getState().getBusState();
    }

    public RobotBusStateMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RobotBusStateMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientEntity(message.entityId, RobotEntity.class,
                (robot) -> robot.getState().setBusStateClient(message.value)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readEnumValue(AbstractDeviceBusController.BusState.class);
    }

    public static void toBytes(final RobotBusStateMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeEnumValue(message.value);
    }
}

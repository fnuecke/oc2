package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.VirtualMachineState;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class RobotRunStateMessage {
    private int entityId;
    private VirtualMachineState.RunState value;

    ///////////////////////////////////////////////////////////////////

    public RobotRunStateMessage(final RobotEntity robot) {
        this.entityId = robot.getEntityId();
        this.value = robot.getState().getRunState();
    }

    public RobotRunStateMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RobotRunStateMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientEntity(message.entityId, RobotEntity.class,
                (robot) -> robot.getState().setRunStateClient(message.value)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readEnumValue(VirtualMachineState.RunState.class);
    }

    public static void toBytes(final RobotRunStateMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeEnumValue(message.value);
    }
}

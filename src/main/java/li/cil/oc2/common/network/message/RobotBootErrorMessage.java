package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public final class RobotBootErrorMessage {
    private int entityId;
    private ITextComponent value;

    ///////////////////////////////////////////////////////////////////

    public RobotBootErrorMessage(final RobotEntity robot) {
        this.entityId = robot.getEntityId();
        this.value = robot.getVirtualMachine().getBootError();
    }

    public RobotBootErrorMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final RobotBootErrorMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withClientEntity(message.entityId, RobotEntity.class,
                (robot) -> robot.getVirtualMachine().setBootErrorClient(message.value)));
        return true;
    }

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readTextComponent();
    }

    public static void toBytes(final RobotBootErrorMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeTextComponent(message.value);
    }
}

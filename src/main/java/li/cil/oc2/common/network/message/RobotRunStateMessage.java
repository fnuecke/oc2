package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.VMRunState;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RobotRunStateMessage extends AbstractMessage {
    private int entityId;
    private VMRunState value;

    ///////////////////////////////////////////////////////////////////

    public RobotRunStateMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
        this.value = robot.getVirtualMachine().getRunState();
    }

    public RobotRunStateMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readEnum(VMRunState.class);
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientEntity(entityId, RobotEntity.class,
                (robot) -> robot.getVirtualMachine().setRunStateClient(value));
    }
}

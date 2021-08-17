package li.cil.oc2.common.network.message;

import li.cil.oc2.common.bus.CommonDeviceBusController;
import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RobotBusStateMessage extends AbstractMessage {
    private int entityId;
    private CommonDeviceBusController.BusState value;

    ///////////////////////////////////////////////////////////////////

    public RobotBusStateMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
        this.value = robot.getVirtualMachine().getBusState();
    }

    public RobotBusStateMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readEnum(CommonDeviceBusController.BusState.class);
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
                (robot) -> robot.getVirtualMachine().setBusStateClient(value));
    }
}

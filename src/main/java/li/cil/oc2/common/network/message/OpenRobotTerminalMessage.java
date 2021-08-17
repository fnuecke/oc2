package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class OpenRobotTerminalMessage extends AbstractMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public OpenRobotTerminalMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
    }

    public OpenRobotTerminalMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeVarInt(entityId);
    }

    ///////////////////////////////////////////////////////////////////


    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerEntity(context, entityId, RobotEntity.class,
                (robot) -> robot.openTerminalScreen(context.getSender()));
    }
}

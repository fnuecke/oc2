package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.network.Network;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RobotInitializationRequestMessage extends AbstractMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public RobotInitializationRequestMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
    }

    public RobotInitializationRequestMessage(final PacketBuffer buffer) {
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
        MessageUtils.withServerEntity(context, entityId, RobotEntity.class,
                (robot) -> Network.INSTANCE.reply(new RobotInitializationMessage(robot), context));
    }
}

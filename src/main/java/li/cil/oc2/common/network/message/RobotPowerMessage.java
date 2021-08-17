package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RobotPowerMessage extends AbstractMessage {
    private int entityId;
    private boolean power;

    ///////////////////////////////////////////////////////////////////

    public RobotPowerMessage(final RobotEntity robot, final boolean power) {
        this.entityId = robot.getId();
        this.power = power;
    }

    public RobotPowerMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        power = buffer.readBoolean();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(power);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerEntity(context, entityId, RobotEntity.class,
                (robot) -> {
                    if (power) {
                        robot.start();
                    } else {
                        robot.stop();
                    }
                });
    }
}

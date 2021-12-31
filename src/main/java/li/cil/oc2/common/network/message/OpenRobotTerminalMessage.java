package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenRobotTerminalMessage extends AbstractMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public OpenRobotTerminalMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
    }

    public OpenRobotTerminalMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
    }

    ///////////////////////////////////////////////////////////////////


    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        final ServerPlayer player = context.getSender();
        if (player != null) {
            MessageUtils.withNearbyServerEntity(context, entityId, RobotEntity.class,
                (robot) -> robot.openTerminalScreen(player));
        }
    }
}

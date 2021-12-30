package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public final class OpenRobotInventoryMessage extends AbstractMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public OpenRobotInventoryMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
    }

    public OpenRobotInventoryMessage(final FriendlyByteBuf buffer) {
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
        MessageUtils.withNearbyServerEntity(context, entityId, RobotEntity.class,
                (robot) -> robot.openInventoryScreen(context.getSender()));
    }
}

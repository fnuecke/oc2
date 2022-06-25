/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public final class OpenRobotInventoryMessage extends AbstractMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public OpenRobotInventoryMessage(final Robot robot) {
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
        final ServerPlayer player = context.getSender();
        if (player != null) {
            MessageUtils.withNearbyServerEntity(context, entityId, Robot.class,
                robot -> robot.openInventoryScreen(player));
        }
    }
}

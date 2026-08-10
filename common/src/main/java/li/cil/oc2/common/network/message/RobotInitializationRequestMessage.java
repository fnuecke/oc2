/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.architectury.networking.NetworkManager;

public final class RobotInitializationRequestMessage extends AbstractMessage {
    private int entityId;

    ///////////////////////////////////////////////////////////////////

    public RobotInitializationRequestMessage(final Robot robot) {
        this.entityId = robot.getId();
    }

    public RobotInitializationRequestMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withServerEntity(context, entityId, Robot.class,
            robot -> reply(new RobotInitializationMessage(robot), context));
    }
}

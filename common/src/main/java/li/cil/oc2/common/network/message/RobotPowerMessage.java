/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class RobotPowerMessage extends AbstractMessage {
    private int entityId;
    private boolean power;

    // ------------------------------------------------------------- //

    public RobotPowerMessage(final Robot robot, final boolean power) {
        this.entityId = robot.getId();
        this.power = power;
    }

    public RobotPowerMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // ------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        power = buffer.readBoolean();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeBoolean(power);
    }

    // ------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerEntity(context, entityId, Robot.class,
                robot -> {
                    if (power) {
                        robot.start();
                    } else {
                        robot.stop();
                    }
                });
    }
}

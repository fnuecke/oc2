/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.PowerAction;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class RobotPowerMessage extends AbstractMessage {
    private int entityId;
    private PowerAction action;

    // --------------------------------------------------------------------- //

    public RobotPowerMessage(final Robot robot, final PowerAction action) {
        this.entityId = robot.getId();
        this.action = action;
    }

    public RobotPowerMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        action = buffer.readEnum(PowerAction.class);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeEnum(action);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerEntity(context, entityId, Robot.class,
            robot -> {
                switch (action) {
                    case START -> robot.start();
                    case SHUTDOWN -> robot.shutdown();
                    case REBOOT -> robot.reboot();
                    case POWER_OFF -> robot.stop();
                }
            });
    }
}

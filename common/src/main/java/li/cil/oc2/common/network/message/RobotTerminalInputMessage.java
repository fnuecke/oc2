/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.nio.ByteBuffer;

public final class RobotTerminalInputMessage extends AbstractTerminalEntityMessage {
    public RobotTerminalInputMessage(final Robot robot, final ByteBuffer data) {
        super(robot, data);
    }

    public RobotTerminalInputMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // ------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withNearbyServerEntity(context, entityId, Robot.class,
                robot -> robot.getTerminal().putInput(ByteBuffer.wrap(data)));
    }
}

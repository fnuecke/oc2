/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.Robot;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.architectury.networking.NetworkManager;

import java.nio.ByteBuffer;

public final class RobotTerminalOutputMessage extends AbstractTerminalEntityMessage {
    public RobotTerminalOutputMessage(final Robot robot, final ByteBuffer data) {
        super(robot, data);
    }

    public RobotTerminalOutputMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientEntity(entityId, Robot.class,
            robot -> robot.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;

public final class RobotTerminalOutputMessage extends AbstractTerminalEntityMessage {
    public RobotTerminalOutputMessage(final RobotEntity robot, final ByteBuffer data) {
        super(robot, data);
    }

    public RobotTerminalOutputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientEntity(entityId, RobotEntity.class,
                robot -> robot.getTerminal().putOutput(ByteBuffer.wrap(data)));
    }
}

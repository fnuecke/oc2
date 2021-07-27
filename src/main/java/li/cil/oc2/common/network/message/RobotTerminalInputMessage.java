package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;

public final class RobotTerminalInputMessage extends AbstractTerminalEntityMessage {
    public RobotTerminalInputMessage(final RobotEntity robot, final ByteBuffer data) {
        super(robot, data);
    }

    public RobotTerminalInputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withNearbyServerEntity(context, entityId, RobotEntity.class,
                (tileEntity) -> tileEntity.getTerminal().putInput(ByteBuffer.wrap(data)));
    }
}

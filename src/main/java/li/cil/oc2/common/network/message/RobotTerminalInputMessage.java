package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

public final class RobotTerminalInputMessage extends AbstractTerminalEntityMessage {
    public RobotTerminalInputMessage(final RobotEntity robot, final ByteBuffer data) {
        super(robot, data);
    }

    public RobotTerminalInputMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public static boolean handleMessage(final AbstractTerminalEntityMessage message, final Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> MessageUtils.withServerEntity(context, message.entityId, RobotEntity.class,
                (tileEntity) -> tileEntity.getTerminal().putInput(ByteBuffer.wrap(message.data))));
        return true;
    }
}

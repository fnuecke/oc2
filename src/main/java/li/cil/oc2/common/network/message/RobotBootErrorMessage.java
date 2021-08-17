package li.cil.oc2.common.network.message;

import li.cil.oc2.common.entity.RobotEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

public final class RobotBootErrorMessage extends AbstractMessage {
    private int entityId;
    private ITextComponent value;

    ///////////////////////////////////////////////////////////////////

    public RobotBootErrorMessage(final RobotEntity robot) {
        this.entityId = robot.getId();
        this.value = robot.getVirtualMachine().getBootError();
    }

    public RobotBootErrorMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        value = buffer.readComponent();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeComponent(value);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientEntity(entityId, RobotEntity.class,
                (robot) -> robot.getVirtualMachine().setBootErrorClient(value));
    }
}

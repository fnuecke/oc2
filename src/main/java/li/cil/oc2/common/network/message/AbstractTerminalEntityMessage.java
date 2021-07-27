package li.cil.oc2.common.network.message;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalEntityMessage extends AbstractMessage {
    protected int entityId;
    protected byte[] data;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalEntityMessage(final Entity entity, final ByteBuffer data) {
        this.entityId = entity.getId();
        this.data = data.array();
    }

    protected AbstractTerminalEntityMessage(final PacketBuffer buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final PacketBuffer buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeByteArray(data);
    }
}

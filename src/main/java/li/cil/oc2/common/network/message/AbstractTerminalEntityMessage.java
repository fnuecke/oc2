package li.cil.oc2.common.network.message;

import net.minecraft.entity.Entity;
import net.minecraft.network.PacketBuffer;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalEntityMessage {
    protected int entityId;
    protected byte[] data;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalEntityMessage(final Entity entity, final ByteBuffer data) {
        this.entityId = entity.getEntityId();
        this.data = data.array();
    }

    protected AbstractTerminalEntityMessage(final PacketBuffer buffer) {
        fromBytes(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    public void fromBytes(final PacketBuffer buffer) {
        entityId = buffer.readVarInt();
        data = buffer.readByteArray();
    }

    public static void toBytes(final AbstractTerminalEntityMessage message, final PacketBuffer buffer) {
        buffer.writeVarInt(message.entityId);
        buffer.writeByteArray(message.data);
    }
}

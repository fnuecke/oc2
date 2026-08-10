/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.Entity;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalEntityMessage extends AbstractMessage {
    protected int entityId;
    protected byte[] data;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTerminalEntityMessage(final Entity entity, final ByteBuffer data) {
        this.entityId = entity.getId();
        this.data = data.array();
    }

    protected AbstractTerminalEntityMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        entityId = buffer.readVarInt();
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(entityId);
        buffer.writeByteArray(data);
    }
}

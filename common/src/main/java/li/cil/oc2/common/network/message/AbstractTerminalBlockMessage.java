/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.nio.ByteBuffer;

public abstract class AbstractTerminalBlockMessage extends AbstractMessage {
    protected BlockPos pos;
    protected byte[] data;

    // --------------------------------------------------------------------- //

    protected AbstractTerminalBlockMessage(final BlockPos pos, final ByteBuffer data) {
        this.pos = pos;
        this.data = data.array();
    }

    protected AbstractTerminalBlockMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        data = buffer.readByteArray();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByteArray(data);
    }
}

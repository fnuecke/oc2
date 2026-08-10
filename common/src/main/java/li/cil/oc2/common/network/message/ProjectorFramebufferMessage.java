/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import dev.architectury.networking.NetworkManager;

import java.nio.ByteBuffer;

public final class ProjectorFramebufferMessage extends AbstractMessage {
    private BlockPos pos;
    private ByteBuffer frame;

    ///////////////////////////////////////////////////////////////////

    public ProjectorFramebufferMessage(final BlockPos projectorPos, final ByteBuffer frame) {
        this.pos = projectorPos;
        this.frame = frame;
    }

    public ProjectorFramebufferMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        frame = ByteBuffer.allocateDirect(buffer.readVarInt());
        buffer.readBytes(frame);
        frame.flip();
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(frame.limit());
        buffer.writeBytes(frame);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
            projector -> projector.applyNextFrameClient(frame));
    }
}

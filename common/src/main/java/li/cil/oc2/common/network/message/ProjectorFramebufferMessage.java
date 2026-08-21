/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.network.message;

import dev.architectury.networking.NetworkManager;
import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public final class ProjectorFramebufferMessage extends AbstractMessage {
    private BlockPos pos;
    private byte[] frame;

    // --------------------------------------------------------------------- //

    public ProjectorFramebufferMessage(final BlockPos projectorPos, final byte[] frame) {
        this.pos = projectorPos;
        this.frame = frame;
    }

    public ProjectorFramebufferMessage(final RegistryFriendlyByteBuf buffer) {
        super(buffer);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void fromBytes(final RegistryFriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        frame = buffer.readByteArray(ProjectorBlockEntity.MAX_FRAME_SIZE);
    }

    @Override
    public void toBytes(final RegistryFriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeByteArray(frame);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void handleMessage(final NetworkManager.PacketContext context) {
        MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
            projector -> projector.applyNextFrameClient(frame));
    }
}

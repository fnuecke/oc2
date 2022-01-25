package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;

public final class ProjectorFramebufferMessage extends AbstractMessage {
    private BlockPos pos;
    private ByteBuffer frame;

    ///////////////////////////////////////////////////////////////////

    public ProjectorFramebufferMessage(final ProjectorBlockEntity projector, final ByteBuffer frame) {
        this.pos = projector.getBlockPos();
        this.frame = frame;
    }

    public ProjectorFramebufferMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        frame = ByteBuffer.allocateDirect(buffer.readVarInt());
        buffer.readBytes(frame);
        frame.flip();
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(frame.limit());
        buffer.writeBytes(frame);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        context.enqueueWork(() ->
            MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
                projector -> projector.applyNextFrame(frame)));
    }
}

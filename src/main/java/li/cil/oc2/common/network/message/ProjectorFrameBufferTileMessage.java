package li.cil.oc2.common.network.message;

import li.cil.oc2.common.blockentity.ProjectorBlockEntity;
import li.cil.oc2.common.network.MessageUtils;
import li.cil.oc2.common.vm.device.SimpleFramebufferDevice;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public final class ProjectorFrameBufferTileMessage extends AbstractMessage {
    private BlockPos pos;
    private SimpleFramebufferDevice.Tile tile;

    ///////////////////////////////////////////////////////////////////

    public ProjectorFrameBufferTileMessage(final ProjectorBlockEntity projector, final SimpleFramebufferDevice.Tile tile) {
        this.pos = projector.getBlockPos();
        this.tile = tile;
    }

    public ProjectorFrameBufferTileMessage(final FriendlyByteBuf buffer) {
        super(buffer);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void fromBytes(final FriendlyByteBuf buffer) {
        pos = buffer.readBlockPos();
        final int startPixelX = buffer.readVarInt();
        final int startPixelY = buffer.readVarInt();
        final ByteBuffer data = ByteBuffer.allocate(SimpleFramebufferDevice.TILE_SIZE_IN_BYTES).order(ByteOrder.LITTLE_ENDIAN);
        buffer.readBytes(data);
        data.flip();
        tile = new SimpleFramebufferDevice.Tile(startPixelX, startPixelY, data);
    }

    @Override
    public void toBytes(final FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(tile.startPixelX());
        buffer.writeVarInt(tile.startPixelY());
        buffer.writeBytes(tile.data());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void handleMessage(final NetworkEvent.Context context) {
        MessageUtils.withClientBlockEntityAt(pos, ProjectorBlockEntity.class,
            projector -> projector.applyFramebufferTile(tile));
    }
}

package li.cil.oc2.client.audio;

import li.cil.oc2.common.Constants;
import net.minecraft.client.audio.TickableSound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

public final class LoopingTileEntitySound extends TickableSound {
    private static final float FADE_IN_DURATION = 2.0f;

    ///////////////////////////////////////////////////////////////////

    private final TileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public LoopingTileEntitySound(final TileEntity tileEntity, final SoundEvent sound) {
        super(sound, SoundCategory.BLOCKS);
        this.tileEntity = tileEntity;
        this.volume = 0;

        final Vector3d position = Vector3d.copyCentered(tileEntity.getPos());
        x = position.x;
        y = position.y;
        z = position.z;

        repeat = true;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        volume = MathHelper.clamp(volume + FADE_IN_DURATION / Constants.TICK_SECONDS, 0, 1);
        final ChunkPos chunkPos = new ChunkPos(tileEntity.getPos());
        if (tileEntity.isRemoved() || !tileEntity.getWorld().chunkExists(chunkPos.x, chunkPos.z)) {
            finishPlaying();
        }
    }
}

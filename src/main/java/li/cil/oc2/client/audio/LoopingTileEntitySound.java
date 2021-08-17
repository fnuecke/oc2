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
    private static final float FADE_IN_DURATION_IN_SECONDS = 2.0f;
    private static final float FADE_IN_DURATION_IN_TICKS = FADE_IN_DURATION_IN_SECONDS * Constants.SECONDS_TO_TICKS;
    private static final float FADE_IN_PER_TICK = 1f / FADE_IN_DURATION_IN_TICKS;

    ///////////////////////////////////////////////////////////////////

    private final TileEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public LoopingTileEntitySound(final TileEntity tileEntity, final SoundEvent sound) {
        super(sound, SoundCategory.BLOCKS);
        this.tileEntity = tileEntity;
        this.volume = 0;

        final Vector3d position = Vector3d.atCenterOf(tileEntity.getBlockPos());
        x = position.x;
        y = position.y;
        z = position.z;

        looping = true;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        volume = MathHelper.clamp(volume + FADE_IN_PER_TICK, 0, 1);
        final ChunkPos chunkPos = new ChunkPos(tileEntity.getBlockPos());
        if (tileEntity.isRemoved() || !tileEntity.getLevel().hasChunk(chunkPos.x, chunkPos.z)) {
            stop();
        }
    }
}

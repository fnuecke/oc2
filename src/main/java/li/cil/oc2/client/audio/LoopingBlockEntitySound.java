package li.cil.oc2.client.audio;

import li.cil.oc2.common.Constants;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

public final class LoopingBlockEntitySound extends AbstractTickableSoundInstance {
    private static final float FADE_IN_DURATION = 2.0f;

    ///////////////////////////////////////////////////////////////////

    private final BlockEntity tileEntity;

    ///////////////////////////////////////////////////////////////////

    public LoopingBlockEntitySound(final BlockEntity tileEntity, final SoundEvent sound) {
        super(sound, SoundSource.BLOCKS);
        this.tileEntity = tileEntity;
        this.volume = 0;

        final Vec3 position = Vec3.atCenterOf(tileEntity.getBlockPos());
        x = position.x;
        y = position.y;
        z = position.z;

        looping = true;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        volume = Mth.clamp(volume + FADE_IN_DURATION / Constants.TICK_SECONDS, 0, 1);
        final ChunkPos chunkPos = new ChunkPos(tileEntity.getBlockPos());
        if (tileEntity.isRemoved() || !tileEntity.getLevel().hasChunk(chunkPos.x, chunkPos.z)) {
            stop();
        }
    }
}

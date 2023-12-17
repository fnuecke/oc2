/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.audio;

import li.cil.oc2.common.util.TickUtils;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;

import java.time.Duration;

public final class LoopingBlockEntitySound extends AbstractTickableSoundInstance {
    private static final float FADE_IN_DURATION_IN_TICKS = TickUtils.toTicks(Duration.ofSeconds(2));
    private static final float FADE_IN_PER_TICK = 1f / FADE_IN_DURATION_IN_TICKS;

    ///////////////////////////////////////////////////////////////////

    private final BlockEntity blockEntity;
    private boolean isCanceled;

    ///////////////////////////////////////////////////////////////////

    public LoopingBlockEntitySound(final BlockEntity blockEntity, final SoundEvent sound, final RandomSource random) {
        super(sound, SoundSource.BLOCKS, random);
        this.blockEntity = blockEntity;
        this.volume = 0;

        final Vec3 position = Vec3.atCenterOf(blockEntity.getBlockPos());
        x = position.x;
        y = position.y;
        z = position.z;

        looping = true;
    }

    ///////////////////////////////////////////////////////////////////

    public void cancel() {
        isCanceled = true;
    }

    @Override
    public void tick() {
        volume = Mth.clamp(volume + FADE_IN_PER_TICK, 0, 1);
        final ChunkPos chunkPos = new ChunkPos(blockEntity.getBlockPos());
        if (blockEntity.isRemoved() || blockEntity.getLevel() == null || !blockEntity.getLevel().hasChunk(chunkPos.x, chunkPos.z)) {
            stop();
        }
    }

    @Override
    public boolean canPlaySound() {
        return !isCanceled;
    }
}

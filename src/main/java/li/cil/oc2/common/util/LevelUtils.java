package li.cil.oc2.common.util;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class LevelUtils {
    @Nullable
    public static BlockEntity getBlockEntityIfChunkExists(final LevelAccessor level, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return level.getBlockEntity(pos);
    }

    @Nullable
    public static String getBlockName(final LevelAccessor level, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            final ResourceLocation registryName = blockEntity.getType().getRegistryName();
            if (registryName != null) {
                return registryName.toString();
            }
        }

        final Block block = level.getBlockState(pos).getBlock();
        {
            final ResourceLocation registryName = block.getRegistryName();
            if (registryName != null) {
                return registryName.toString();
            }
        }

        if (blockEntity != null) {
            return blockEntity.getClass().getSimpleName();
        }

        return block.getClass().getSimpleName();
    }

    public static void playSound(final LevelAccessor level, final BlockPos pos, final SoundType soundType, final Function<SoundType, SoundEvent> soundEvent) {
        playSound(level, pos, soundType, soundEvent.apply(soundType));
    }

    public static void playSound(final LevelAccessor level, final BlockPos pos, final SoundType soundType, final SoundEvent soundEvent) {
        playSound(level, pos, soundEvent, SoundSource.BLOCKS, (soundType.getVolume() + 1f) / 2f, soundType.getPitch() * 0.8f);
    }

    public static void playSound(final LevelAccessor level, final BlockPos pos, final SoundEvent soundEvent, final SoundSource soundCategory, final float volume, final float pitch) {
        level.playSound(null, pos, soundEvent, soundCategory, volume, pitch);
    }
}

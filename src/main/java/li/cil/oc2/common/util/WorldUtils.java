package li.cil.oc2.common.util;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class WorldUtils {
    @Nullable
    public static BlockEntity getBlockEntityIfChunkExists(final LevelAccessor world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return world.getBlockEntity(pos);
    }

    @Nullable
    public static String getBlockName(final LevelAccessor world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity != null) {
            final ResourceLocation registryName = tileEntity.getType().getRegistryName();
            if (registryName != null) {
                return registryName.toString();
            }
        }

        final Block block = world.getBlockState(pos).getBlock();
        {
            final ResourceLocation registryName = block.getRegistryName();
            if (registryName != null) {
                return registryName.toString();
            }
        }

        if (tileEntity != null) {
            return tileEntity.getClass().getSimpleName();
        }

        return block.getClass().getSimpleName();
    }

    public static void playSound(final LevelAccessor world, final BlockPos pos, final SoundType soundType, final Function<SoundType, SoundEvent> soundEvent) {
        playSound(world, pos, soundType, soundEvent.apply(soundType));
    }

    public static void playSound(final LevelAccessor world, final BlockPos pos, final SoundType soundType, final SoundEvent soundEvent) {
        playSound(world, pos, soundEvent, SoundSource.BLOCKS, (soundType.getVolume() + 1f) / 2f, soundType.getPitch() * 0.8f);
    }

    public static void playSound(final LevelAccessor world, final BlockPos pos, final SoundEvent soundEvent, final SoundSource soundCategory, final float volume, final float pitch) {
        world.playSound(null, pos, soundEvent, soundCategory, volume, pitch);
    }
}

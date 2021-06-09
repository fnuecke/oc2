package li.cil.oc2.common.util;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;
import java.util.function.Function;

public final class WorldUtils {
    @Nullable
    public static TileEntity getBlockEntityIfChunkExists(final IWorld world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return world.getBlockEntity(pos);
    }

    @Nullable
    public static String getBlockName(final IWorld world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.hasChunk(chunkPos.x, chunkPos.z)) {
            return null;
        }

        final TileEntity tileEntity = world.getBlockEntity(pos);
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

    public static void playSound(final IWorld world, final BlockPos pos, final SoundType soundType, final Function<SoundType, SoundEvent> soundEvent) {
        playSound(world, pos, soundType, soundEvent.apply(soundType));
    }

    public static void playSound(final IWorld world, final BlockPos pos, final SoundType soundType, final SoundEvent soundEvent) {
        playSound(world, pos, soundEvent, SoundCategory.BLOCKS, (soundType.getVolume() + 1f) / 2f, soundType.getPitch() * 0.8f);
    }

    public static void playSound(final IWorld world, final BlockPos pos, final SoundEvent soundEvent, final SoundCategory soundCategory, final float volume, final float pitch) {
        world.playSound(null, pos, soundEvent, soundCategory, volume, pitch);
    }
}

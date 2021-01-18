package li.cil.oc2.common.util;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.IWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public final class WorldUtils {
    @Nullable
    public static TileEntity getTileEntityIfChunkExists(final IWorld world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return world.getTileEntity(pos);
    }

    @Nullable
    public static String getBlockName(final IWorld world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
            return null;
        }

        final TileEntity tileEntity = world.getTileEntity(pos);
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

    public static void playSound(final IWorld world, final BlockPos pos, final SoundType soundType, Function<SoundType, SoundEvent> soundEvent) {
        playSound(world, pos, soundType, soundEvent.apply(soundType));
    }

    public static void playSound(final IWorld world, final BlockPos pos, final SoundType soundType, final SoundEvent soundEvent) {
        world.playSound(null, pos, soundEvent, SoundCategory.BLOCKS, (soundType.getVolume() + 1f) / 2f, soundType.getPitch() * 0.8f);
    }
}

package li.cil.oc2.common.util;

import net.minecraft.block.Block;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public final class WorldUtils {
    @Nullable
    public static TileEntity getTileEntityIfChunkExists(final World world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.chunkExists(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return world.getTileEntity(pos);
    }

    @Nullable
    public static String getBlockName(final World world, final BlockPos pos) {
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
}

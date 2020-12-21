package li.cil.oc2.common.util;

import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.WorldAccess;
import org.jetbrains.annotations.Nullable;

public final class WorldUtils {
    @Nullable
    public static BlockEntity getTileEntityIfChunkExists(final WorldAccess world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return null;
        }

        return world.getBlockEntity(pos);
    }

    @Nullable
    public static String getBlockName(final WorldAccess world, final BlockPos pos) {
        final ChunkPos chunkPos = new ChunkPos(pos);
        if (!world.isChunkLoaded(chunkPos.x, chunkPos.z)) {
            return null;
        }

        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity != null) {
            final Identifier registryName = Registry.BLOCK_ENTITY_TYPE.getId(tileEntity.getType());
            if (registryName != null) {
                return registryName.toString();
            }
        }

        final Block block = world.getBlockState(pos).getBlock();
        {
            final Identifier registryName = Registry.BLOCK.getId(block);
            if (registryName != Registry.BLOCK.getDefaultId()) {
                return registryName.toString();
            }
        }

        if (tileEntity != null) {
            return tileEntity.getClass().getSimpleName();
        }

        return block.getClass().getSimpleName();
    }
}

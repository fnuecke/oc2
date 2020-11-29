package li.cil.oc2.common.util;

import net.minecraft.tileentity.TileEntity;
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
}

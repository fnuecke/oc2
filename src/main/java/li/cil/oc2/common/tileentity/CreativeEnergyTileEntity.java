package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CreativeEnergyTileEntity extends BlockEntity {
    private final Direction[] SIDES = Direction.values();

    ///////////////////////////////////////////////////////////////////

    public CreativeEnergyTileEntity(final BlockPos pos, final BlockState state) {
        super(TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    public static void serverTick(final Level level, final BlockPos pos, final BlockState state, final CreativeEnergyTileEntity tileEntity) {
        tileEntity.serverTick();
    }

    private void serverTick() {
        for (final Direction side : SIDES) {
            final BlockPos neighborPos = getBlockPos().relative(side);
            final ChunkPos neighborChunkPos = new ChunkPos(neighborPos);
            if (level.hasChunk(neighborChunkPos.x, neighborChunkPos.z)) {
                final BlockEntity tileEntity = level.getBlockEntity(neighborPos);
                if (tileEntity != null) {
                    tileEntity.getCapability(Capabilities.ENERGY_STORAGE, side.getOpposite()).ifPresent(energy -> {
                        energy.receiveEnergy(Integer.MAX_VALUE, false);
                    });
                }
            }
        }
    }
}

package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.ITickableTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class CreativeEnergyTileEntity extends TileEntity implements ITickableTileEntity {
    private final Direction[] SIDES = Direction.values();

    ///////////////////////////////////////////////////////////////////

    public CreativeEnergyTileEntity() {
        super(TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        for (final Direction side : SIDES) {
            final BlockPos neighborPos = getBlockPos().relative(side);
            final ChunkPos neighborChunkPos = new ChunkPos(neighborPos);
            if (getLevel().hasChunk(neighborChunkPos.x, neighborChunkPos.z)) {
                final TileEntity tileEntity = getLevel().getBlockEntity(neighborPos);
                if (tileEntity != null) {
                    tileEntity.getCapability(Capabilities.ENERGY_STORAGE, side.getOpposite()).ifPresent(energy -> {
                        energy.receiveEnergy(Integer.MAX_VALUE, false);
                    });
                }
            }
        }
    }
}

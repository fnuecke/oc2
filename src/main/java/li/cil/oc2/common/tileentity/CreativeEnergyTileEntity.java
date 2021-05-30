package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.tileentity.ITickableBlockEntity;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;

public final class CreativeEnergyBlockEntity extends BlockEntity implements ITickableBlockEntity {
    private final Direction[] SIDES = Direction.values();

    ///////////////////////////////////////////////////////////////////

    public CreativeEnergyBlockEntity() {
        super(TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void tick() {
        for (final Direction side : SIDES) {
            final BlockPos neighborPos = getBlockPos().relative(side);
            final ChunkPos neighborChunkPos = new ChunkPos(neighborPos);
            if (getLevel().hasChunk(neighborChunkPos.x, neighborChunkPos.z)) {
                final BlockEntity tileEntity = getLevel().getBlockEntity(neighborPos);
                if (tileEntity != null) {
                    tileEntity.getCapability(Capabilities.ENERGY_STORAGE, side.getOpposite()).ifPresent(energy -> {
                        energy.receiveEnergy(Integer.MAX_VALUE, false);
                    });
                }
            }
        }
    }
}

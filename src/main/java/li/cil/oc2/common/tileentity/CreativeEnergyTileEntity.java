package li.cil.oc2.common.tileentity;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.TickableBlockEntity;

public final class CreativeEnergyBlockEntity extends BlockEntity implements TickableBlockEntity {
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

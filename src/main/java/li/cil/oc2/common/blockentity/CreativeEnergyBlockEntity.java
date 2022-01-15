package li.cil.oc2.common.blockentity;

import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class CreativeEnergyBlockEntity extends BlockEntity {
    private final Direction[] SIDES = Direction.values();

    ///////////////////////////////////////////////////////////////////

    public CreativeEnergyBlockEntity(final BlockPos pos, final BlockState state) {
        super(BlockEntities.CREATIVE_ENERGY.get(), pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    public static void serverTick(final Level ignoredLevel, final BlockPos ignoredPos, final BlockState ignoredState, final CreativeEnergyBlockEntity creativeEnergy) {
        creativeEnergy.serverTick();
    }

    private void serverTick() {
        if (level == null) {
            return;
        }

        for (final Direction side : SIDES) {
            final BlockPos neighborPos = getBlockPos().relative(side);
            final ChunkPos neighborChunkPos = new ChunkPos(neighborPos);
            if (level.hasChunk(neighborChunkPos.x, neighborChunkPos.z)) {
                final BlockEntity blockEntity = level.getBlockEntity(neighborPos);
                if (blockEntity != null) {
                    blockEntity.getCapability(Capabilities.ENERGY_STORAGE, side.getOpposite()).ifPresent(energy ->
                        energy.receiveEnergy(Integer.MAX_VALUE, false));
                }
            }
        }
    }
}

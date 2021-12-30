package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.CreativeEnergyTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import li.cil.oc2.common.util.BlockEntityUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public final class CreativeEnergyBlock extends Block implements EntityBlock {
    public CreativeEnergyBlock() {
        super(Properties
            .of(Material.METAL)
            .sound(SoundType.METAL)
            .strength(-1, 3600000)
            .noDrops());
    }

    ///////////////////////////////////////////////////////////////////
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return level.isClientSide ? null : BlockEntityUtils.createTicker(type, TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get(), CreativeEnergyTileEntity::serverTick);
    }
}

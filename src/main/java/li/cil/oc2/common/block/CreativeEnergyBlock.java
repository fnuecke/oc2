package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

public final class CreativeEnergyBlock extends BaseEntityBlock {
    public CreativeEnergyBlock() {
        super(Properties
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(-1, 3600000)
                .noDrops());
    }

    ///////////////////////////////////////////////////////////////////

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockGetter blockGetter) {
        return TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get().create();
    }
}

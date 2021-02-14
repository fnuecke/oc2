package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockReader;

public final class CreativeEnergyBlock extends Block {
    public CreativeEnergyBlock() {
        super(Properties
                .create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(-1, 3600000)
                .noDrops());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.CREATIVE_ENERGY_TILE_ENTITY.get().create();
    }
}

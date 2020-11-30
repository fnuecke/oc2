package li.cil.oc2.common.block;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.common.tile.BusCableTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;

import javax.annotation.Nullable;

public class BusCableBlock extends Block {
    public BusCableBlock() {
        super(Properties.create(Material.IRON).sound(SoundType.METAL));
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return OpenComputers.BUS_CABLE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final World world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final TileEntity tileEntity = world.getTileEntity(pos);
        if (tileEntity instanceof BusCableTileEntity) {
            final BusCableTileEntity busCable = (BusCableTileEntity) tileEntity;
            busCable.handleNeighborChanged(changedBlockPos);
        }
    }
}

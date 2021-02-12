package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public final class ChargerBlock extends BreakableBlock {
    public ChargerBlock() {
        super(Properties
                .create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(1.5f, 6.0f));
        setDefaultState(getStateContainer().getBaseState().with(HorizontalBlock.HORIZONTAL_FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(final BlockState state, final Rotation rot) {
        return state.with(HorizontalBlock.HORIZONTAL_FACING, rot.rotate(state.get(HorizontalBlock.HORIZONTAL_FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(final BlockState state, final Mirror mirrorIn) {
        return state.rotate(mirrorIn.toRotation(state.get(HorizontalBlock.HORIZONTAL_FACING)));
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.CHARGER_TILE_ENTITY.get().create();
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.getDefaultState().with(HorizontalBlock.HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(HorizontalBlock.HORIZONTAL_FACING);
    }
}

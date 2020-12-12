package li.cil.oc2.common.block;

import li.cil.oc2.OpenComputers;
import li.cil.oc2.common.block.entity.RedstoneInterfaceTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;

import javax.annotation.Nullable;

public final class RedstoneInterfaceBlock extends HorizontalBlock {
    public RedstoneInterfaceBlock() {
        super(Properties.create(Material.IRON).sound(SoundType.METAL));
        setDefaultState(getStateContainer().getBaseState().with(HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.getDefaultState().with(HORIZONTAL_FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Nullable
    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return OpenComputers.REDSTONE_INTERFACE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean canProvidePower(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getWeakPower(final BlockState state, final IBlockReader world, final BlockPos pos, final Direction side) {
        if (side.getAxis().getPlane() == Direction.Plane.HORIZONTAL) {
            final TileEntity tileEntity = world.getTileEntity(pos);
            if (tileEntity instanceof RedstoneInterfaceTileEntity) {
                final RedstoneInterfaceTileEntity redstoneInterface = (RedstoneInterfaceTileEntity) tileEntity;
                // Redstone requests info for faces with external perspective. We treat
                // the Direction from internal perspective, so flip it.
                return redstoneInterface.getOutputForDirection(side.getOpposite());
            }
        }
        return super.getWeakPower(state, world, pos, side);
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getStrongPower(final BlockState state, final IBlockReader world, final BlockPos pos, final Direction side) {
        if (side == Direction.NORTH || side == Direction.EAST || side == Direction.SOUTH || side == Direction.WEST) {
            return getWeakPower(state, world, pos, side);
        } else {
            return super.getStrongPower(state, world, pos, side);
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void fillStateContainer(final StateContainer.Builder<Block, BlockState> builder) {
        super.fillStateContainer(builder);
        builder.add(HORIZONTAL_FACING);
    }
}

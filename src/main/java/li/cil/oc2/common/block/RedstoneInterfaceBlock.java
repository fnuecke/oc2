package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.RedstoneInterfaceTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
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
import net.minecraft.world.IWorldReader;

public final class RedstoneInterfaceBlock extends HorizontalBlock {
    public RedstoneInterfaceBlock() {
        super(Properties
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockState getStateForPlacement(final BlockItemUseContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean hasTileEntity(final BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(final BlockState state, final IBlockReader world) {
        return TileEntities.REDSTONE_INTERFACE_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(final BlockState state, final IBlockReader world, final BlockPos pos, final Direction side) {
        final TileEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof RedstoneInterfaceTileEntity) {
            final RedstoneInterfaceTileEntity redstoneInterface = (RedstoneInterfaceTileEntity) tileEntity;
            // Redstone requests info for faces with external perspective. We treat
            // the Direction from internal perspective, so flip it.
            return redstoneInterface.getOutputForDirection(side.getOpposite());
        }

        return super.getSignal(state, world, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(final BlockState state, final IWorldReader world, final BlockPos pos, final Direction side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(final BlockState state, final IBlockReader world, final BlockPos pos, final Direction side) {
        return getSignal(state, world, pos, side);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}

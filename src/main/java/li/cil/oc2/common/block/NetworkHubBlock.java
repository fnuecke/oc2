package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.NetworkHubBlockEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalDirectionalBlock;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.BlockEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;

public final class NetworkHubBlock extends HorizontalDirectionalBlock {
    public NetworkHubBlock() {
        super(Properties
                .of(Material.METAL)
                .sound(SoundType.METAL)
                .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean hasBlockEntity(final BlockState state) {
        return true;
    }

    @Override
    public BlockEntity createBlockEntity(final BlockState state, final BlockGetter world) {
        return TileEntities.NETWORK_HUB_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final Level world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof NetworkHubBlockEntity) {
            final NetworkHubBlockEntity hub = (NetworkHubBlockEntity) tileEntity;
            hub.handleNeighborChanged();
        }
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateContainer.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}

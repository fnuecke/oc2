package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.RedstoneInterfaceTileEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Material;

import javax.annotation.Nullable;

public final class RedstoneInterfaceBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public RedstoneInterfaceBlock() {
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

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSignalSource(final BlockState state) {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getSignal(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
        final BlockEntity tileEntity = world.getBlockEntity(pos);
        if (tileEntity instanceof RedstoneInterfaceTileEntity) {
            final RedstoneInterfaceTileEntity redstoneInterface = (RedstoneInterfaceTileEntity) tileEntity;
            // Redstone requests info for faces with external perspective. We treat
            // the Direction from internal perspective, so flip it.
            return redstoneInterface.getOutputForDirection(side.getOpposite());
        }

        return super.getSignal(state, world, pos, side);
    }

    @Override
    public boolean shouldCheckWeakPower(final BlockState state, final LevelReader world, final BlockPos pos, final Direction side) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public int getDirectSignal(final BlockState state, final BlockGetter world, final BlockPos pos, final Direction side) {
        return getSignal(state, world, pos, side);
    }

    ///////////////////////////////////////////////////////////////////
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return TileEntities.REDSTONE_INTERFACE_TILE_ENTITY.get().create(pos, state);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FaceAttachedHorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Objects;

public final class NetworkConnectorBlock extends FaceAttachedHorizontalDirectionalBlock implements EntityBlock {
    private static final VoxelShape NEG_Z_SHAPE = Block.box(5, 5, 7, 11, 11, 16);
    private static final VoxelShape POS_Z_SHAPE = Block.box(5, 5, 0, 11, 11, 9);
    private static final VoxelShape NEG_X_SHAPE = Block.box(7, 5, 5, 16, 11, 11);
    private static final VoxelShape POS_X_SHAPE = Block.box(0, 5, 5, 9, 11, 11);
    private static final VoxelShape NEG_Y_SHAPE = Block.box(5, 0, 5, 11, 9, 11);
    private static final VoxelShape POS_Y_SHAPE = Block.box(5, 7, 5, 11, 16, 11);

    ///////////////////////////////////////////////////////////////////

    public NetworkConnectorBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(FACE, AttachFace.WALL));
    }

    ///////////////////////////////////////////////////////////////////

    public static Direction getFacing(final BlockState state) {
        return FaceAttachedHorizontalDirectionalBlock.getConnectedDirection(state);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final Level level, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        if (Objects.equals(changedBlockPos, pos.relative(getFacing(state).getOpposite()))) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final NetworkConnectorBlockEntity networkConnector) {
                networkConnector.setNeighborChanged();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return switch (state.getValue(FACE)) {
            case WALL -> switch (state.getValue(FACING)) {
                case EAST -> POS_X_SHAPE;
                case WEST -> NEG_X_SHAPE;
                case SOUTH -> POS_Z_SHAPE;
                default /* NORTH */ -> NEG_Z_SHAPE;
            };
            case CEILING -> POS_Y_SHAPE;
            default /* FLOOR */ -> NEG_Y_SHAPE;
        };
    }

    ///////////////////////////////////////////////////////////////////
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.NETWORK_CONNECTOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.NETWORK_CONNECTOR.get());
    }

    ///////////////////////////////////////////////////////////////////

    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }
}

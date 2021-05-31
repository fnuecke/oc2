package li.cil.oc2.common.block;

import li.cil.oc2.common.tileentity.NetworkConnectorBlockEntity;
import li.cil.oc2.common.tileentity.TileEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

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
                .of(Material.METAL)
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


    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockGetter blockGetter) {
        return TileEntities.NETWORK_CONNECTOR_TILE_ENTITY.get().create();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final BlockState state, final Level world, final BlockPos pos, final Block changedBlock, final BlockPos changedBlockPos, final boolean isMoving) {
        if (Objects.equals(changedBlockPos, pos.relative(getFacing(state).getOpposite()))) {
            final BlockEntity tileEntity = world.getBlockEntity(pos);
            if (tileEntity instanceof NetworkConnectorBlockEntity) {
                final NetworkConnectorBlockEntity connector = (NetworkConnectorBlockEntity) tileEntity;
                connector.setLocalInterfaceChanged();
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter world, final BlockPos pos, final CollisionContext context) {
        switch (state.getValue(FACE)) {
            case WALL:
                switch (state.getValue(FACING)) {
                    case EAST:
                        return POS_X_SHAPE;
                    case WEST:
                        return NEG_X_SHAPE;
                    case SOUTH:
                        return POS_Z_SHAPE;
                    case NORTH:
                    default:
                        return NEG_Z_SHAPE;
                }
            case CEILING:
                return POS_Y_SHAPE;
            case FLOOR:
            default:
                return NEG_Y_SHAPE;
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.util.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Map;

public abstract class OrientableBlock extends Block {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    // --------------------------------------------------------------------- //

    private final Map<Direction, VoxelShape> shapes;

    // --------------------------------------------------------------------- //

    protected OrientableBlock(final Properties properties, final Direction facing, final int depth) {
        super(properties);
        this.shapes = shapesWithDepth(depth);
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, facing)
            .setValue(ROTATION, 0));
    }

    // --------------------------------------------------------------------- //

    public static int getRotationX(final BlockState state) {
        return switch (state.getValue(FACING)) {
            case DOWN -> 90;
            case UP -> 270;
            default -> 0;
        };
    }

    public static int getRotationY(final BlockState state) {
        final Direction facing = state.getValue(FACING);
        return facing.getAxis().isVertical()
            ? state.getValue(ROTATION) * 90
            : (int) facing.getOpposite().toYRot();
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final BlockPlacement placement = getPlacement(context);
        return defaultBlockState()
            .setValue(FACING, placement.facing())
            .setValue(ROTATION, getRotationForPlacement(placement.facing(), placement.upward()));
    }

    // --------------------------------------------------------------------- //

    protected BlockPlacement getPlacement(final BlockPlaceContext context) {
        return BlockPlacement.free(context);
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return shapes.get(state.getValue(FACING));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ROTATION);
    }

    // --------------------------------------------------------------------- //

    private static int getRotationForPlacement(final Direction facing, final Direction upward) {
        if (facing.getAxis().isHorizontal()) {
            return 0;
        }

        return (int) (facing == Direction.DOWN ? upward.getOpposite() : upward).toYRot() / 90;
    }

    private static Map<Direction, VoxelShape> shapesWithDepth(final int depth) {
        return Map.of(
            Direction.UP, box(0, 0, 0, 16, depth, 16),
            Direction.DOWN, box(0, 16 - depth, 0, 16, 16, 16),
            Direction.NORTH, box(0, 0, 16 - depth, 16, 16, 16),
            Direction.SOUTH, box(0, 0, 0, 16, 16, depth),
            Direction.EAST, box(0, 0, 0, depth, 16, 16),
            Direction.WEST, box(16 - depth, 0, 0, 16, 16, 16)
        );
    }
}

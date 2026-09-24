/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.EnumMap;
import java.util.Map;

public abstract class FlippableOrientableBlock extends Block {
    public static final EnumProperty<FlippableOrientation> ORIENTATION = EnumProperty.create("facing", FlippableOrientation.class);

    // --------------------------------------------------------------------- //

    private final Map<FlippableOrientation, VoxelShape> shapes;

    // --------------------------------------------------------------------- //

    protected FlippableOrientableBlock(final Properties properties, final FlippableOrientation orientation, final VoxelShape northShape) {
        super(properties);
        this.shapes = shapesRotatedFrom(northShape);
        registerDefaultState(getStateDefinition().any()
            .setValue(ORIENTATION, orientation));
    }

    // --------------------------------------------------------------------- //

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction horizontalDirection = context.getHorizontalDirection();
        final Direction facing = context.getPlayer() != null
            ? context.getNearestLookingDirection().getOpposite()
            : horizontalDirection.getOpposite();
        final Direction up = switch (facing) {
            case UP -> horizontalDirection.getOpposite();
            case DOWN -> horizontalDirection;
            default -> context.getClickedFace() == Direction.DOWN ? Direction.DOWN : Direction.UP;
        };
        return defaultBlockState().setValue(ORIENTATION, FlippableOrientation.of(facing, up));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return shapes.get(state.getValue(ORIENTATION));
    }

    @Override
    protected BlockState rotate(final BlockState state, final Rotation rotation) {
        return state.setValue(ORIENTATION, state.getValue(ORIENTATION).rotate(rotation));
    }

    @Override
    protected BlockState mirror(final BlockState state, final Mirror mirror) {
        return state.setValue(ORIENTATION, state.getValue(ORIENTATION).mirror(mirror));
    }

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ORIENTATION);
    }

    // --------------------------------------------------------------------- //

    private static Map<FlippableOrientation, VoxelShape> shapesRotatedFrom(final VoxelShape northShape) {
        final Map<FlippableOrientation, VoxelShape> shapes = new EnumMap<>(FlippableOrientation.class);
        for (final FlippableOrientation orientation : FlippableOrientation.values()) {
            shapes.put(orientation, VoxelShapeUtils.rotateAroundCenter(northShape, orientation.getRotation()));
        }
        return shapes;
    }
}

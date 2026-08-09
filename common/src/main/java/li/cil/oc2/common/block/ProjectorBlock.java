/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.util.VoxelShapeUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

public final class ProjectorBlock extends HorizontalDirectionalBlock implements EntityBlock, EnergyConsumingBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    // We bake the visual indents on the front and sides into the collision shape, to prevent stuff being
    // placeable on those sides, such as network connectors, torches, etc.
    private static final VoxelShape NEG_Z_SHAPE = Shapes.join(Shapes.block(), Shapes.or(
        Shapes.box(0 / 16f, 2 / 16f, 2 / 16f, 1 / 16f, 6 / 16f, 14 / 16f),
        Shapes.box(15 / 16f, 2 / 16f, 2 / 16f, 16 / 16f, 6 / 16f, 14 / 16f),
        Shapes.box(4 / 16f, 4 / 16f, 0 / 16f, 12 / 16f, 12 / 16f, 2 / 16f)
    ), (a, b) -> a && !b);
    private static final VoxelShape NEG_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    private static final VoxelShape POS_Z_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    private static final VoxelShape POS_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);

    public ProjectorBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .lightLevel(state -> state.getValue(LIT) ? 8 : 0)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.NORTH)
            .setValue(LIT, false));
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public int getEnergyConsumption() {
        if (Config.projectorsUseEnergy()) {
            return Config.projectorEnergyPerTick;
        } else {
            return 0;
        }
    }

    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.PROJECTOR.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.PROJECTOR.get());
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos blockPos, final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NEG_Z_SHAPE;
            case SOUTH -> POS_Z_SHAPE;
            case WEST -> NEG_X_SHAPE;
            default -> POS_X_SHAPE;
        };
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    ///////////////////////////////////////////////////////////////////

    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }
}

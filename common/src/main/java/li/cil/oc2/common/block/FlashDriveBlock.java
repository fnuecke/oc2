/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;
import java.util.Map;

public final class FlashDriveBlock extends Block implements EntityBlock {
    public static final MapCodec<FlashDriveBlock> CODEC = MapCodec.unit(FlashDriveBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ROTATION = IntegerProperty.create("rotation", 0, 3);

    private static final Map<Direction, VoxelShape> SHAPES = Map.of(
        Direction.UP, box(0, 0, 0, 16, 8, 16),
        Direction.DOWN, box(0, 8, 0, 16, 16, 16),
        Direction.NORTH, box(0, 0, 8, 16, 16, 16),
        Direction.SOUTH, box(0, 0, 0, 16, 16, 8),
        Direction.EAST, box(0, 0, 0, 8, 16, 16),
        Direction.WEST, box(8, 0, 0, 16, 16, 16)
    );

    // --------------------------------------------------------------------- //

    public FlashDriveBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any()
            .setValue(FACING, Direction.UP)
            .setValue(ROTATION, 0));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected MapCodec<? extends FlashDriveBlock> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        final Direction top = context.getClickedFace();
        return defaultBlockState()
            .setValue(FACING, top)
            .setValue(ROTATION, getRotationForPlacement(top, context.getHorizontalDirection()));
    }

    private static int getRotationForPlacement(final Direction top, final Direction horizontalDirection) {
        if (top.getAxis().isHorizontal()) {
            return 0;
        }

        final int away = (int) horizontalDirection.toYRot();
        final int degrees = top == Direction.UP ? away + 180 : away;
        return degrees % 360 / 90;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final FlashDriveBlockEntity drive) {
            final ItemInteractionResult result = drive.useWith(level, heldStack, player, hand);
            if (result != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
                return result;
            }
        }

        return super.useItemOn(heldStack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final FlashDriveBlockEntity drive) {
            final InteractionResult result = drive.useWithoutItem(level, player);
            if (result != InteractionResult.PASS) {
                return result;
            }
        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }

    // --------------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.FLASH_DRIVE.get().create(pos, state);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING, ROTATION);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final FlashDriveBlockEntity drive) {
                drive.dropMedia();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

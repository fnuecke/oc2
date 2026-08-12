/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.client.gui.KeyboardScreen;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.KeyboardBlockEntity;
import li.cil.oc2.common.util.VoxelShapeUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public final class KeyboardBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<KeyboardBlock> CODEC = MapCodec.unit(KeyboardBlock::new);

    @Override
    protected MapCodec<? extends KeyboardBlock> codec() {
        return CODEC;
    }

    private static final VoxelShape NEG_Z_SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 8, 16), // main body
            Block.box(0, 8, 8, 16, 12, 16) // top
    );
    private static final VoxelShape NEG_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_Z_SHAPE);
    private static final VoxelShape POS_Z_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(NEG_X_SHAPE);
    private static final VoxelShape POS_X_SHAPE = VoxelShapeUtils.rotateHorizontalClockwise(POS_Z_SHAPE);

    // ------------------------------------------------------------- //

    public KeyboardBlock() {
        super(Properties.of()
                .mapColor(MapColor.METAL).sound(SoundType.METAL).strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    // ------------------------------------------------------------- //

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(final BlockState state, final BlockGetter level, final BlockPos pos, final CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> NEG_Z_SHAPE;
            case SOUTH -> POS_Z_SHAPE;
            case WEST -> NEG_X_SHAPE;
            default -> POS_X_SHAPE;
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final KeyboardBlockEntity keyboard)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (level.isClientSide()) {
            openKeyboardScreen(keyboard);
        }

        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final KeyboardBlockEntity keyboard)) {
            return super.useWithoutItem(state, level, pos, player, hit);
        }

        if (level.isClientSide()) {
            openKeyboardScreen(keyboard);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    // ------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.KEYBOARD.get().create(pos, state);
    }

    // ------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    // ------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    private static void openKeyboardScreen(final KeyboardBlockEntity keyboard) {
        final KeyboardScreen screen = new KeyboardScreen(keyboard);
        Minecraft.getInstance().setScreen(screen);
    }
}

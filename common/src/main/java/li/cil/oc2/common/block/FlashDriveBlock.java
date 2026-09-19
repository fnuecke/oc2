/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.FlashDriveBlockEntity;
import li.cil.oc2.common.util.BlockPlacement;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class FlashDriveBlock extends OrientableBlock implements EntityBlock {
    public static final MapCodec<FlashDriveBlock> CODEC = MapCodec.unit(FlashDriveBlock::new);

    // --------------------------------------------------------------------- //

    public FlashDriveBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f), Direction.UP, 8);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected MapCodec<? extends FlashDriveBlock> codec() {
        return CODEC;
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
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final FlashDriveBlockEntity drive) {
                drive.dropMedia();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected BlockPlacement getPlacement(final BlockPlaceContext context) {
        return isPlacingOnBusInterface(context) ? BlockPlacement.attached(context) : BlockPlacement.free(context);
    }

    // --------------------------------------------------------------------- //

    private static boolean isPlacingOnBusInterface(final BlockPlaceContext context) {
        final Direction clicked = context.getClickedFace();
        final BlockState state = context.getLevel().getBlockState(context.getClickedPos().relative(clicked.getOpposite()));
        return state.getBlock() instanceof BusCableBlock
            && BusCableBlock.getConnectionType(state, clicked) == BusCableBlock.ConnectionType.INTERFACE;
    }

}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.DiskDriveBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
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

import javax.annotation.Nullable;

public final class DiskDriveBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public static final MapCodec<DiskDriveBlock> CODEC = MapCodec.unit(DiskDriveBlock::new);

    @Override
    protected MapCodec<? extends DiskDriveBlock> codec() {
        return CODEC;
    }

    public DiskDriveBlock() {
        super(Properties
                .of()
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL)
                .strength(1.5f, 6.0f));
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    // ------------------------------------------------------------- //

    @Override
    public BlockState getStateForPlacement(final BlockPlaceContext context) {
        return super.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack heldStack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof final DiskDriveBlockEntity diskDrive)) {
            return super.useItemOn(heldStack, state, level, pos, player, hand, hit);
        }

        if (player.isShiftKeyDown()) {
            if (diskDrive.canEject()) {
                if (!level.isClientSide()) {
                    diskDrive.eject(player);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        } else {
            if (diskDrive.canInsert(heldStack)) {
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, diskDrive.insert(heldStack, player));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.useItemOn(heldStack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final DiskDriveBlockEntity diskDrive && player.isShiftKeyDown() && diskDrive.canEject()) {
            if (!level.isClientSide()) {
                diskDrive.eject(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }

    // ------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.DISK_DRIVE.get().create(pos, state);
    }

    // ------------------------------------------------------------- //

    @Override
    protected void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
    }

    @Override
    protected void onRemove(final BlockState state, final Level level, final BlockPos pos, final BlockState newState, final boolean movedByPiston) {
        if (!state.is(newState.getBlock())) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof final DiskDriveBlockEntity drive) {
                drive.dropFloppy();
            }
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}

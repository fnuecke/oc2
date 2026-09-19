/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.block;

import com.mojang.serialization.MapCodec;
import li.cil.oc2.client.gui.TerminalConfigurationScreen;
import li.cil.oc2.client.gui.TerminalScreen;
import li.cil.oc2.common.blockentity.BlockEntities;
import li.cil.oc2.common.blockentity.TerminalBlockEntity;
import li.cil.oc2.common.blockentity.TickableBlockEntity;
import li.cil.oc2.common.integration.Wrenches;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

public final class TerminalBlock extends OrientableBlock implements EntityBlock {
    public static final MapCodec<TerminalBlock> CODEC = MapCodec.unit(TerminalBlock::new);

    // --------------------------------------------------------------------- //

    public TerminalBlock() {
        super(Properties
            .of()
            .mapColor(MapColor.METAL)
            .sound(SoundType.METAL)
            .strength(1.5f, 6.0f), Direction.NORTH, 6);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected MapCodec<? extends TerminalBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(final ItemStack stack, final BlockState state, final Level level, final BlockPos pos, final Player player, final InteractionHand hand, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (Wrenches.isWrench(stack) && blockEntity instanceof final TerminalBlockEntity terminal) {
            if (player.isShiftKeyDown()) {
                return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
            }

            if (level.isClientSide()) {
                openConfigurationScreen(terminal);
            }

            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(final BlockState state, final Level level, final BlockPos pos, final Player player, final BlockHitResult hit) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof final TerminalBlockEntity terminal) {
            if (level.isClientSide()) {
                openTerminalScreen(terminal);
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return super.useWithoutItem(state, level, pos, player, hit);
    }

    // --------------------------------------------------------------------- //
    // EntityBlock

    @Nullable
    @Override
    public BlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
        return BlockEntities.TERMINAL.get().create(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
        return TickableBlockEntity.createServerTicker(level, type, BlockEntities.TERMINAL.get());
    }

    // --------------------------------------------------------------------- //

    @Environment(EnvType.CLIENT)
    private static void openTerminalScreen(final TerminalBlockEntity terminal) {
        Minecraft.getInstance().setScreen(new TerminalScreen(terminal));
    }

    @Environment(EnvType.CLIENT)
    private static void openConfigurationScreen(final TerminalBlockEntity terminal) {
        Minecraft.getInstance().setScreen(new TerminalConfigurationScreen(terminal));
    }

}

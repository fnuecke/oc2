/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.tags.BlockTags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Objects;

public final class WrenchItem extends ModItem {
    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final Direction face = context.getClickedFace();
        if (face == Direction.UP || face == Direction.DOWN) {
            final BlockState blockState = level.getBlockState(pos);
            final BlockState rotatedState = blockState.rotate(level, pos, face == Direction.UP ? Rotation.CLOCKWISE_90 : Rotation.COUNTERCLOCKWISE_90);
            if (!Objects.equals(blockState, rotatedState)) {
                level.setBlockAndUpdate(pos, rotatedState);
                return InteractionResult.sidedSuccess(level.isClientSide());
            }
        }

        return super.onItemUseFirst(stack, context);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }

        if (!player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);
        if (!state.is(BlockTags.WRENCH_BREAKABLE)) {
            return super.useOn(context);
        }

        if (level.isClientSide()) {
            final MultiPlayerGameMode gameMode = Minecraft.getInstance().gameMode;
            if (gameMode != null) {
                gameMode.destroyBlock(pos);
            }
        } else if (player instanceof final ServerPlayer serverPlayer) {
            serverPlayer.gameMode.destroyBlock(pos);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final LevelReader level, final BlockPos pos, final Player player) {
        return true;
    }
}

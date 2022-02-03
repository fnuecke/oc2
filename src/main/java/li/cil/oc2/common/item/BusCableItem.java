/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.util.LevelUtils;
import li.cil.oc2.common.util.TooltipUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public final class BusCableItem extends ModBlockItem {
    public BusCableItem(final Block block) {
        super(block);
    }

    ///////////////////////////////////////////////////////////////////

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(final ItemStack stack, final @Nullable Level level, final List<Component> tooltip, final TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        TooltipUtils.addEnergyConsumption(Config.busCableEnergyPerTick, tooltip);
    }

    @Override
    public InteractionResult useOn(final UseOnContext context) {
        final InteractionResult result = tryAddToBlock(context);
        return result.consumesAction() ? result : super.useOn(context);
    }

    @Override
    public InteractionResult place(final BlockPlaceContext context) {
        final InteractionResult result = tryAddToBlock(context);
        return result.consumesAction() ? result : super.place(context);
    }

    ///////////////////////////////////////////////////////////////////

    private static InteractionResult tryAddToBlock(final UseOnContext context) {
        final Level level = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = level.getBlockState(pos);

        if (!BusCableBlock.addCable(level, pos, state)) {
            return InteractionResult.PASS;
        }

        final Player player = context.getPlayer();
        final ItemStack stack = context.getItemInHand();

        if (player instanceof final ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, pos, stack);
        }

        LevelUtils.playSound(level, pos, state.getSoundType(level, pos, player), SoundType::getPlaceSound);

        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}

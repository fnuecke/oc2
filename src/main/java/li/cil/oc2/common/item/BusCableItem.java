package li.cil.oc2.common.item;

import li.cil.oc2.common.Config;
import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.util.TooltipUtils;
import li.cil.oc2.common.util.WorldUtils;
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
import org.jetbrains.annotations.Nullable;

import java.util.List;

public final class BusCableItem extends ModBlockItem {
    public BusCableItem(final Block block) {
        super(block);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public void appendHoverText(final ItemStack itemStack, final @Nullable Level level, final List<Component> list, final TooltipFlag tooltipFlag) {
        super.appendHoverText(itemStack, level, list, tooltipFlag);
        TooltipUtils.addEnergyConsumption(Config.busCableEnergyPerTick, list);
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

    private InteractionResult tryAddToBlock(final UseOnContext context) {
        final BusCableBlock busCableBlock = Blocks.BUS_CABLE;

        final Level world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = world.getBlockState(pos);

        if (state.getBlock() == busCableBlock && busCableBlock.addCable(world, pos, state)) {
            final Player player = context.getPlayer();
            final ItemStack stack = context.getItemInHand();

            if (player instanceof ServerPlayer) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayer) player, pos, stack);
            }

            WorldUtils.playSound(world, pos, state.getSoundType(), SoundType::getPlaceSound);

            if (player == null || !player.abilities.instabuild) {
                stack.shrink(1);
            }

            return InteractionResult.sidedSuccess(world.isClientSide);
        }

        return InteractionResult.PASS;
    }
}

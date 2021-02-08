package li.cil.oc2.common.item;

import li.cil.oc2.common.block.Blocks;
import li.cil.oc2.common.block.BusCableBlock;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class BusCableItem extends ModBlockItem {
    public BusCableItem(final Block block) {
        super(block);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final ActionResultType result = tryAddToBlock(context);
        return result.isSuccessOrConsume() ? result : super.onItemUse(context);
    }

    @Override
    public ActionResultType tryPlace(final BlockItemUseContext context) {
        final ActionResultType result = tryAddToBlock(context);
        return result.isSuccessOrConsume() ? result : super.tryPlace(context);
    }

    ///////////////////////////////////////////////////////////////////

    private ActionResultType tryAddToBlock(final ItemUseContext context) {
        final BusCableBlock busCableBlock = Blocks.BUS_CABLE.get();

        final World world = context.getWorld();
        final BlockPos pos = context.getPos();
        final BlockState state = world.getBlockState(pos);

        if (state.getBlock() == busCableBlock && busCableBlock.addCable(world, pos, state)) {
            final PlayerEntity player = context.getPlayer();
            final ItemStack stack = context.getItem();

            if (player instanceof ServerPlayerEntity) {
                CriteriaTriggers.PLACED_BLOCK.trigger((ServerPlayerEntity) player, pos, stack);
            }

            WorldUtils.playSound(world, pos, state.getSoundType(world, pos, player), SoundType::getPlaceSound);

            if (player == null || !player.abilities.isCreativeMode) {
                stack.shrink(1);
            }

            return ActionResultType.func_233537_a_(world.isRemote);
        }

        return ActionResultType.PASS;
    }
}

package li.cil.oc2.common.item;

import li.cil.oc2.common.tags.BlockTags;
import net.minecraft.block.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public final class WrenchItem extends ModItem {
    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final PlayerEntity player = context.getPlayer();
        if (!player.isSneaking()) {
            return super.onItemUse(context);
        }

        final World world = context.getWorld();
        final BlockPos pos = context.getPos();
        final BlockState state = world.getBlockState(pos);
        if (!state.isIn(BlockTags.WRENCH_BREAKABLE)) {
            return super.onItemUse(context);
        }

        if (world.isRemote()) {
            Minecraft.getInstance().playerController.onPlayerDestroyBlock(pos);
        } else if (player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).interactionManager.tryHarvestBlock(pos);
        }

        return ActionResultType.func_233537_a_(world.isRemote);
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final IWorldReader world, final BlockPos pos, final PlayerEntity player) {
        return true;
    }
}

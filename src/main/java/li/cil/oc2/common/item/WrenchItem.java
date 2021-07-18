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
    public ActionResultType useOn(final ItemUseContext context) {
        final PlayerEntity player = context.getPlayer();
        if (!player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        final World world = context.getLevel();
        final BlockPos pos = context.getClickedPos();
        final BlockState state = world.getBlockState(pos);
        if (!state.is(BlockTags.WRENCH_BREAKABLE)) {
            return super.useOn(context);
        }

        if (world.isClientSide()) {
            Minecraft.getInstance().gameMode.destroyBlock(pos);
        } else if (player instanceof ServerPlayerEntity) {
            ((ServerPlayerEntity) player).gameMode.destroyBlock(pos);
        }

        return ActionResultType.sidedSuccess(world.isClientSide());
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final IWorldReader world, final BlockPos pos, final PlayerEntity player) {
        return true;
    }
}

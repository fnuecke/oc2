package li.cil.oc2.common.item;

import li.cil.oc2.api.API;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemUseContext;
import net.minecraft.server.management.PlayerInteractionManager;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Objects;

public final class WrenchItem extends ModItem {
    public WrenchItem(final Properties properties) {
        super(properties);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public ActionResultType onItemUse(final ItemUseContext context) {
        final World world = context.getWorld();
        final BlockPos pos = context.getPos();
        final BlockState state = world.getBlockState(pos);
        final ResourceLocation registryName = state.getBlock().getRegistryName();
        if (registryName == null || !Objects.equals(registryName.getNamespace(), API.MOD_ID)) {
            return super.onItemUse(context);
        }

        final PlayerEntity player = context.getPlayer();
        if (player instanceof ServerPlayerEntity) {
            final PlayerInteractionManager interactionManager = ((ServerPlayerEntity) player).interactionManager;
            if (interactionManager.tryHarvestBlock(pos)) {
                WorldUtils.playSound(world, pos, state.getSoundType(), SoundType::getBreakSound);
                return ActionResultType.SUCCESS;
            }
        }

        return super.onItemUse(context);
    }
}

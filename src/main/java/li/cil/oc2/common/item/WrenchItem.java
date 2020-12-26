package li.cil.oc2.common.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

public final class WrenchItem extends Item {
    public WrenchItem(final Properties properties) {
        super(properties);
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final IWorldReader world, final BlockPos pos, final PlayerEntity player) {
        return true;
    }
}

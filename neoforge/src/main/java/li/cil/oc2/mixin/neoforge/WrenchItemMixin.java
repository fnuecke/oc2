/* SPDX-License-Identifier: MIT */

package li.cil.oc2.mixin.neoforge;

import li.cil.oc2.common.item.WrenchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(WrenchItem.class)
public abstract class WrenchItemMixin extends Item {
    private WrenchItemMixin(final Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onItemUseFirst(final ItemStack stack, final UseOnContext context) {
        return ((WrenchItem) (Object) this).tryRotate(context);
    }

    @Override
    public boolean doesSneakBypassUse(final ItemStack stack, final LevelReader level, final BlockPos pos, final Player player) {
        // The wrench's shift-click actions live in the blocks' own interaction handlers, so sneaking
        // must not skip block interaction the way it normally does with a non-empty hand.
        return true;
    }
}

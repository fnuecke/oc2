/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fabric;

import li.cil.oc2.common.item.WrenchItem;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/**
 * Matches NeoForge's {@code IItemExtension#onItemUseFirst}.
 */
public final class WrenchInteractionFabric {
    public static void initialize() {
        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            final ItemStack stack = player.getItemInHand(hand);
            if (!(stack.getItem() instanceof final WrenchItem wrench)) {
                return InteractionResult.PASS;
            }

            return wrench.tryRotate(new UseOnContext(player, hand, hit));
        });
    }

    public static boolean isHoldingWrench(final Player player) {
        for (final InteractionHand hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).getItem() instanceof WrenchItem) {
                return true;
            }
        }
        return false;
    }

    private WrenchInteractionFabric() {
    }
}

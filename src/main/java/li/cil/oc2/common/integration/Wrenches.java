/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.integration;

import li.cil.oc2.common.tags.ItemTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class Wrenches {
    public static boolean isWrench(@Nullable final ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.is(ItemTags.WRENCHES);
    }

    public static boolean isHoldingWrench(final Entity entity) {
        for (final ItemStack stack : entity.getHandSlots()) {
            if (isWrench(stack)) {
                return true;
            }
        }

        return false;
    }
}

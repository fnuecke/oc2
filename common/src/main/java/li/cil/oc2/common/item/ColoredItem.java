/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import net.minecraft.world.item.ItemStack;

/**
 * Implemented by items that are tinted, optionally by a dye stored on the stack.
 */
public interface ColoredItem {
    /**
     * The tint colour for the given stack, as packed RGB.
     *
     * @param stack the stack to get the colour for.
     * @return the packed RGB colour.
     */
    int getColor(ItemStack stack);
}

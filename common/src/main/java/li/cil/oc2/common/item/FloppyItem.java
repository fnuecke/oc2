/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.item;

import li.cil.oc2.common.Constants;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.DyedItemColor;

public final class FloppyItem extends AbstractStorageItem implements ColoredItem {
    public static final int MAX_CAPACITY = 512 * Constants.KILOBYTE;

    public FloppyItem(final int capacity) {
        super(capacity);
    }

    @Override
    public int getColor(final ItemStack stack) {
        return DyedItemColor.getOrDefault(stack, DyedItemColor.LEATHER_COLOR);
    }
}

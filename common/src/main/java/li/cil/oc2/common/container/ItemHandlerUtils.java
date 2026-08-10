/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

public final class ItemHandlerUtils {
    public static ItemStack insertItemStacked(final ItemHandler handler, ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (stack.isStackable()) {
            for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
                final ItemStack existing = handler.getStackInSlot(slot);
                if (!existing.isEmpty() && ItemStack.isSameItemSameComponents(existing, stack)) {
                    stack = handler.insertItem(slot, stack, simulate);
                }
            }

            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
        }

        for (int slot = 0; slot < handler.getSlots() && !stack.isEmpty(); slot++) {
            if (handler.getStackInSlot(slot).isEmpty()) {
                stack = handler.insertItem(slot, stack, simulate);
            }
        }

        return stack;
    }

    private ItemHandlerUtils() {
    }
}

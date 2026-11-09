/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inventory;

import net.minecraft.world.item.ItemStack;

public interface ItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    ItemStack extractItem(int slot, int amount, boolean simulate);

    default int getSlotLimit(final int slot) {
        return 64;
    }

    default boolean isItemValid(final int slot, final ItemStack stack) {
        return true;
    }
}

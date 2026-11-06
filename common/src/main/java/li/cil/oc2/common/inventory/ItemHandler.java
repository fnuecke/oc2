/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inventory;

import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

public interface ItemHandler {
    int getSlots();

    ItemStack getStackInSlot(int slot);

    ItemStack insertItem(int slot, ItemStack stack, TransactionContext transaction);

    ItemStack extractItem(int slot, int amount, TransactionContext transaction);

    default int getSlotLimit(final int slot) {
        return 64;
    }

    default boolean isItemValid(final int slot, final ItemStack stack) {
        return true;
    }
}

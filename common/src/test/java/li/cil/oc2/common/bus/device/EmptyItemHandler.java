/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

record EmptyItemHandler(int slots) implements ItemHandler {
    @Override
    public int getSlots() {
        return slots;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        return stack;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return ItemStack.EMPTY;
    }
}

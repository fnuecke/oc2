/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

public record CachedItemHandler(CapabilityCache<ItemHandler> cache) implements ItemHandler {
    @Override
    public int getSlots() {
        return handler().getSlots();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return handler().getStackInSlot(slot);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        return handler().insertItem(slot, stack, simulate);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return handler().extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return handler().getSlotLimit(slot);
    }

    private ItemHandler handler() {
        final ItemHandler handler = cache.get();
        if (handler == null) {
            throw new IllegalStateException("inventory is gone");
        }
        return handler;
    }
}

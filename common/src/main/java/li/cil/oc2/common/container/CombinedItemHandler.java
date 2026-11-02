/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.inventory.DeferredNotifyItemHandler;
import net.minecraft.world.item.ItemStack;

public final class CombinedItemHandler implements DeferredNotifyItemHandler {
    private final DeferredNotifyItemHandler[] handlers;
    private final int[] baseSlot;
    private final int slotCount;

    // --------------------------------------------------------------------- //

    public CombinedItemHandler(final DeferredNotifyItemHandler... handlers) {
        this.handlers = handlers;
        this.baseSlot = new int[handlers.length];

        int slots = 0;
        for (int i = 0; i < handlers.length; i++) {
            baseSlot[i] = slots;
            slots += handlers[i].getSlots();
        }
        this.slotCount = slots;
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getSlots() {
        return slotCount;
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final int index = indexOf(slot);
        return handlers[index].getStackInSlot(slot - baseSlot[index]);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        final int index = indexOf(slot);
        return handlers[index].insertItem(slot - baseSlot[index], stack, simulate);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        final int index = indexOf(slot);
        return handlers[index].extractItem(slot - baseSlot[index], amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        final int index = indexOf(slot);
        return handlers[index].getSlotLimit(slot - baseSlot[index]);
    }

    @Override
    public boolean isItemValid(final int slot, final ItemStack stack) {
        final int index = indexOf(slot);
        return handlers[index].isItemValid(slot - baseSlot[index], stack);
    }

    @Override
    public void setStackInSlotSilently(final int slot, final ItemStack stack) {
        final int index = indexOf(slot);
        handlers[index].setStackInSlotSilently(slot - baseSlot[index], stack);
    }

    @Override
    public void notifySlotChanged(final int slot) {
        final int index = indexOf(slot);
        handlers[index].notifySlotChanged(slot - baseSlot[index]);
    }

    // --------------------------------------------------------------------- //

    private int indexOf(final int slot) {
        if (slot < 0 || slot >= slotCount) {
            throw new IndexOutOfBoundsException("Slot " + slot + " not in range [0," + slotCount + ")");
        }

        for (int i = handlers.length - 1; i >= 0; i--) {
            if (slot >= baseSlot[i]) {
                return i;
            }
        }

        throw new IllegalStateException();
    }
}

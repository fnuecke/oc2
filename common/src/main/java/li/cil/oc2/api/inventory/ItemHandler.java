/* SPDX-License-Identifier: MIT */

package li.cil.oc2.api.inventory;

import net.minecraft.world.item.ItemStack;

/**
 * A slotted item container supporting simulated insertion and extraction.
 */
public interface ItemHandler {
    /**
     * The number of slots in this container.
     *
     * @return the slot count.
     */
    int getSlots();

    /**
     * The stack in the specified slot.
     * <p>
     * The returned stack must not be modified; copy it first.
     *
     * @param slot the slot to read.
     * @return the stack in the slot, or {@link ItemStack#EMPTY}.
     */
    ItemStack getStackInSlot(int slot);

    /**
     * Inserts a stack into the specified slot.
     *
     * @param slot     the slot to insert into.
     * @param stack    the stack to insert; not modified by this call.
     * @param simulate whether to only test the operation instead of performing it.
     * @return the remainder that could not be inserted; {@link ItemStack#EMPTY} if all of it was.
     */
    ItemStack insertItem(int slot, ItemStack stack, boolean simulate);

    /**
     * Extracts up to {@code amount} items from the specified slot.
     *
     * @param slot     the slot to extract from.
     * @param amount   the maximum number of items to extract.
     * @param simulate whether to only test the operation instead of performing it.
     * @return the extracted stack; {@link ItemStack#EMPTY} if nothing was extracted.
     */
    ItemStack extractItem(int slot, int amount, boolean simulate);
}

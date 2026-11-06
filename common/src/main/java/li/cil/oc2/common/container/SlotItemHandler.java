/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.transaction.Transaction;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class SlotItemHandler extends Slot {
    private static final Container EMPTY_CONTAINER = new SimpleContainer(0);

    private final ItemHandler itemHandler;
    private final int index;

    // --------------------------------------------------------------------- //

    public SlotItemHandler(final ItemHandler itemHandler, final int index, final int xPosition, final int yPosition) {
        super(EMPTY_CONTAINER, index, xPosition, yPosition);
        this.itemHandler = itemHandler;
        this.index = index;
    }

    // --------------------------------------------------------------------- //

    @Override
    public boolean mayPlace(final ItemStack stack) {
        if (stack.isEmpty() || !itemHandler.isItemValid(index, stack)) {
            return false;
        }

        final ItemStack existing = getItem();
        return existing.isEmpty() || (ItemStack.isSameItemSameComponents(existing, stack) && existing.getCount() < getMaxStackSize(stack));
    }

    @Override
    public ItemStack getItem() {
        return itemHandler.getStackInSlot(index);
    }

    @Override
    public void set(final ItemStack stack) {
        if (itemHandler instanceof final ItemStackHandler handler) {
            handler.setStackInSlot(index, stack);
        }
        setChanged();
    }

    @Override
    public void onQuickCraft(final ItemStack oldStack, final ItemStack newStack) {
    }

    @Override
    public int getMaxStackSize() {
        return itemHandler instanceof final ItemStackHandler handler
            ? handler.getSlotLimit(index)
            : super.getMaxStackSize();
    }

    @Override
    public boolean mayPickup(final Player player) {
        return !getItem().isEmpty();
    }

    @Override
    public ItemStack remove(final int amount) {
        return Transaction.run(transaction -> itemHandler.extractItem(index, amount, transaction));
    }
}

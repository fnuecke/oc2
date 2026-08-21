/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.inventory.ItemHandler;
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

    public ItemHandler getItemHandler() {
        return itemHandler;
    }

    @Override
    public boolean mayPlace(final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return itemHandler.insertItem(index, stack, true).getCount() < stack.getCount();
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
    public int getMaxStackSize(final ItemStack stack) {
        final ItemStack remainder = itemHandler.insertItem(index, stack.copyWithCount(stack.getMaxStackSize()), true);
        return stack.getMaxStackSize() - remainder.getCount() + getItem().getCount();
    }

    @Override
    public boolean mayPickup(final Player player) {
        return !itemHandler.extractItem(index, 1, true).isEmpty();
    }

    @Override
    public ItemStack remove(final int amount) {
        return itemHandler.extractItem(index, amount, false);
    }
}

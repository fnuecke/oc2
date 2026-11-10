/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public abstract class AbstractContainer extends AbstractContainerMenu {
    protected static final int HOTBAR_SIZE = 9;
    protected static final int PLAYER_INVENTORY_ROWS = 3;
    protected static final int PLAYER_INVENTORY_COLUMNS = 9;

    public AbstractContainer(@Nullable final MenuType<?> type, final int id) {
        super(type, id);
    }

    @Override
    public ItemStack quickMoveStack(final Player player, final int index) {
        final Slot from = slots.get(index);
        final ItemStack stack = from.getItem().copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final boolean intoPlayerInventory = from.container != player.getInventory();
        final ItemStack fromStack = from.getItem();

        final int step, begin;
        if (intoPlayerInventory) {
            step = -1;
            begin = slots.size() - 1;
        } else {
            step = 1;
            begin = 0;
        }

        if (fromStack.getMaxStackSize() > 1) {
            for (int i = begin; i >= 0 && i < slots.size(); i += step) {
                final Slot into = slots.get(i);
                if (into.container == from.container) {
                    continue;
                }

                if (!into.mayPlace(fromStack)) {
                    continue;
                }

                if (!into.hasItem()) {
                    continue;
                }

                final ItemStack intoStack = into.getItem();
                final boolean itemsAreEqual = ItemStack.isSameItemSameComponents(fromStack, intoStack);
                if (!itemsAreEqual) {
                    continue;
                }

                final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(stack));
                final int spaceInSlot = maxSizeInSlot - intoStack.getCount();
                if (spaceInSlot <= 0) {
                    continue;
                }

                final int itemsMoved = Math.min(spaceInSlot, fromStack.getCount());
                if (itemsMoved <= 0) {
                    continue;
                }

                intoStack.grow(from.remove(itemsMoved).getCount());
                into.setChanged();

                if (from.getItem().isEmpty()) {
                    break;
                }
            }
        }

        for (int i = begin; i >= 0 && i < slots.size(); i += step) {
            if (from.getItem().isEmpty()) {
                break;
            }

            final Slot into = slots.get(i);
            if (into.container == from.container) {
                continue;
            }

            if (!into.mayPlace(fromStack)) {
                continue;
            }

            if (into.hasItem()) {
                continue;
            }

            final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(fromStack));
            final int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.set(from.remove(itemsMoved));
        }

        return from.getItem().getCount() < stack.getCount() ? from.getItem() : ItemStack.EMPTY;
    }

    protected void createPlayerInventorySlots(final Inventory inventory) {
        for (int index = 0; index < HOTBAR_SIZE + PLAYER_INVENTORY_ROWS * PLAYER_INVENTORY_COLUMNS; index++) {
            addSlot(isSlotLocked(inventory, index) ? new LockedSlot(inventory, index, 0, 0) : new Slot(inventory, index, 0, 0));
        }
    }

    protected boolean isSlotLocked(final Inventory inventory, final int slot) {
        return false;
    }
}

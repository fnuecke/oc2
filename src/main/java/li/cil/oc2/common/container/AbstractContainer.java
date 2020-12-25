package li.cil.oc2.common.container;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.Slot;
import net.minecraft.item.ItemStack;

import javax.annotation.Nullable;

public abstract class AbstractContainer extends Container {
    protected static final int HOTBAR_SIZE = 9;
    protected static final int SLOT_SIZE = 18;
    protected static final int PLAYER_INVENTORY_ROWS = 3;
    protected static final int PLAYER_INVENTORY_COLUMNS = 9;
    protected static final int PLAYER_INVENTORY_HOTBAR_SPACING = 4;

    public AbstractContainer(@Nullable final ContainerType<?> type, final int id) {
        super(type, id);
    }

    @Override
    public ItemStack transferStackInSlot(final PlayerEntity player, final int index) {
        final Slot from = inventorySlots.get(index);
        if (from == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = from.getStack().copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final boolean intoPlayerInventory = from.inventory != player.inventory;
        final ItemStack fromStack = from.getStack();

        final int step, begin;
        if (intoPlayerInventory) {
            step = -1;
            begin = inventorySlots.size() - 1;
        } else {
            step = 1;
            begin = 0;
        }

        if (fromStack.getMaxStackSize() > 1) {
            for (int i = begin; i >= 0 && i < inventorySlots.size(); i += step) {
                final Slot into = inventorySlots.get(i);
                if (into.inventory == from.inventory) {
                    continue;
                }

                final ItemStack intoStack = into.getStack();
                if (intoStack.isEmpty()) {
                    continue;
                }

                final boolean itemsAreEqual = fromStack.isItemEqual(intoStack) && ItemStack.areItemStackTagsEqual(fromStack, intoStack);
                if (!itemsAreEqual) {
                    continue;
                }

                final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getItemStackLimit(stack));
                final int spaceInSlot = maxSizeInSlot - intoStack.getCount();
                if (spaceInSlot <= 0) {
                    continue;
                }

                final int itemsMoved = Math.min(spaceInSlot, fromStack.getCount());
                if (itemsMoved <= 0) {
                    continue;
                }

                intoStack.grow(from.decrStackSize(itemsMoved).getCount());
                into.onSlotChanged();

                if (from.getStack().isEmpty()) {
                    break;
                }
            }
        }

        for (int i = begin; i >= 0 && i < inventorySlots.size(); i += step) {
            if (from.getStack().isEmpty()) {
                break;
            }

            final Slot into = inventorySlots.get(i);
            if (into.inventory == from.inventory) {
                continue;
            }

            if (into.getHasStack()) {
                continue;
            }

            if (!into.isItemValid(fromStack)) {
                continue;
            }

            final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getItemStackLimit(fromStack));
            final int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.putStack(from.decrStackSize(itemsMoved));
        }

        return from.getStack().getCount() < stack.getCount() ? from.getStack() : ItemStack.EMPTY;
    }

    protected int createPlayerInventoryAndHotbarSlots(final PlayerInventory inventory, final int startX, final int startY) {
        final int nextIndex = createHotbarSlots(inventory, 0, startX, startY + PLAYER_INVENTORY_ROWS * SLOT_SIZE + PLAYER_INVENTORY_HOTBAR_SPACING);
        return createPlayerInventorySlots(inventory, nextIndex, startX, startY);
    }

    protected int createPlayerInventorySlots(final PlayerInventory inventory, final int startIndex, final int startX, final int startY) {
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; ++row) {
            for (int column = 0; column < PLAYER_INVENTORY_COLUMNS; ++column) {
                final int index = startIndex + row * PLAYER_INVENTORY_COLUMNS + column;
                final int x = startX + column * SLOT_SIZE;
                final int y = startY + row * SLOT_SIZE;
                this.addSlot(new Slot(inventory, index, x, y));
            }
        }

        return startIndex + PLAYER_INVENTORY_ROWS * PLAYER_INVENTORY_COLUMNS;
    }

    protected int createHotbarSlots(final PlayerInventory inventory, final int startIndex, final int startX, final int startY) {
        for (int i = 0; i < HOTBAR_SIZE; ++i) {
            this.addSlot(new Slot(inventory, startIndex + i, startX + i * SLOT_SIZE, startY));
        }
        return startIndex + HOTBAR_SIZE;
    }
}

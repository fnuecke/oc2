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
    public ItemStack quickMoveStack(final PlayerEntity player, final int index) {
        final Slot from = slots.get(index);
        if (from == null) {
            return ItemStack.EMPTY;
        }
        final ItemStack stack = from.getItem().copy();
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final boolean intoPlayerInventory = from.container != player.inventory;
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

                final ItemStack intoStack = into.getItem();
                if (intoStack.isEmpty()) {
                    continue;
                }

                final boolean itemsAreEqual = fromStack.sameItem(intoStack) && ItemStack.tagMatches(fromStack, intoStack);
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

            if (into.hasItem()) {
                continue;
            }

            if (!into.mayPlace(fromStack)) {
                continue;
            }

            final int maxSizeInSlot = Math.min(fromStack.getMaxStackSize(), into.getMaxStackSize(fromStack));
            final int itemsMoved = Math.min(maxSizeInSlot, fromStack.getCount());
            into.set(from.remove(itemsMoved));
        }

        return from.getItem().getCount() < stack.getCount() ? from.getItem() : ItemStack.EMPTY;
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

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.inventory.DeferredNotifyItemHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemStackHandler implements DeferredNotifyItemHandler {
    private static final String SIZE_TAG_NAME = "Size";

    protected NonNullList<ItemStack> stacks;

    // --------------------------------------------------------------------- //

    public ItemStackHandler(final int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public ItemStackHandler(final NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getSlots() {
        return stacks.size();
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        validateSlot(slot);
        return stacks.get(slot);
    }

    public void setStackInSlot(final int slot, final ItemStack stack) {
        validateSlot(slot);
        stacks.set(slot, stack);
        onContentsChanged(slot);
    }

    @Override
    public void setStackInSlotSilently(final int slot, final ItemStack stack) {
        validateSlot(slot);
        stacks.set(slot, stack);
    }

    @Override
    public void notifySlotChanged(final int slot) {
        onContentsChanged(slot);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        if (!isItemValid(slot, stack)) {
            return stack;
        }

        validateSlot(slot);

        final ItemStack existing = stacks.get(slot);

        int limit = getStackLimit(slot, stack);
        if (!existing.isEmpty()) {
            if (!ItemStack.isSameItemSameComponents(stack, existing)) {
                return stack;
            }
            limit -= existing.getCount();
        }

        if (limit <= 0) {
            return stack;
        }

        final boolean reachedLimit = stack.getCount() > limit;
        if (!simulate) {
            if (existing.isEmpty()) {
                stacks.set(slot, reachedLimit ? stack.copyWithCount(limit) : stack);
            } else {
                existing.grow(reachedLimit ? limit : stack.getCount());
            }
            onContentsChanged(slot);
        }

        return reachedLimit ? stack.copyWithCount(stack.getCount() - limit) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (amount == 0) {
            return ItemStack.EMPTY;
        }

        validateSlot(slot);

        final ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final int toExtract = Math.min(amount, existing.getMaxStackSize());
        if (existing.getCount() <= toExtract) {
            if (!simulate) {
                stacks.set(slot, ItemStack.EMPTY);
                onContentsChanged(slot);
                return existing;
            }
            return existing.copy();
        }

        if (!simulate) {
            stacks.set(slot, existing.copyWithCount(existing.getCount() - toExtract));
            onContentsChanged(slot);
        }

        return existing.copyWithCount(toExtract);
    }

    public int getSlotLimit(final int slot) {
        return Item.DEFAULT_MAX_STACK_SIZE;
    }

    public boolean isItemValid(final int slot, final ItemStack stack) {
        return true;
    }

    // --------------------------------------------------------------------- //

    public void save(final ValueOutput output) {
        output.putInt(SIZE_TAG_NAME, stacks.size());
        ContainerHelper.saveAllItems(output, stacks);
    }

    public void load(final ValueInput input) {
        setSize(readSize(input));
        ContainerHelper.loadAllItems(input, stacks);
        onLoad();
    }

    // --------------------------------------------------------------------- //

    protected void setSize(final int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
    }

    protected int readSize(final ValueInput input) {
        return input.getIntOr(SIZE_TAG_NAME, stacks.size());
    }

    protected int getStackLimit(final int slot, final ItemStack stack) {
        return Math.min(getSlotLimit(slot), stack.getMaxStackSize());
    }

    protected void onContentsChanged(final int slot) {
    }

    protected void onLoad() {
    }

    private void validateSlot(final int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IndexOutOfBoundsException("Slot " + slot + " not in range [0," + stacks.size() + ")");
        }
    }
}

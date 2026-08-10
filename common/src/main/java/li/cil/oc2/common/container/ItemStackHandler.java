/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.inventory.ItemHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * oc2's own slotted item container, replacing Forge's {@code ItemStackHandler}.
 * <p>
 * Kept deliberately close to the original's shape so the many subclasses did not have to change,
 * but the NBT methods now take a {@link HolderLookup.Provider}: from 1.20.5 on, item stacks cannot
 * be serialized without registry access.
 */
public class ItemStackHandler implements ItemHandler {
    private static final String SIZE_TAG_NAME = "Size";

    protected NonNullList<ItemStack> stacks;

    ///////////////////////////////////////////////////////////////////

    public ItemStackHandler(final int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public ItemStackHandler(final NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
    }

    ///////////////////////////////////////////////////////////////////

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

    ///////////////////////////////////////////////////////////////////

    public CompoundTag serializeNBT(final HolderLookup.Provider provider) {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(SIZE_TAG_NAME, stacks.size());
        ContainerHelper.saveAllItems(tag, stacks, provider);
        return tag;
    }

    public void deserializeNBT(final HolderLookup.Provider provider, final CompoundTag tag) {
        setSize(tag.contains(SIZE_TAG_NAME) ? tag.getInt(SIZE_TAG_NAME) : stacks.size());
        ContainerHelper.loadAllItems(tag, stacks, provider);
        onLoad();
    }

    ///////////////////////////////////////////////////////////////////

    protected void setSize(final int size) {
        stacks = NonNullList.withSize(size, ItemStack.EMPTY);
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

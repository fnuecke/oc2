/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.transaction.SnapshotJournal;
import li.cil.oc2.common.transaction.TransactionContext;
import li.cil.oc2.jcodec.common.tools.MathUtil;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class ItemStackHandler implements ItemHandler {
    private static final String SIZE_TAG_NAME = "Size";

    protected NonNullList<ItemStack> stacks;
    private SnapshotJournal[] journals;

    // --------------------------------------------------------------------- //

    public ItemStackHandler(final int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public ItemStackHandler(final NonNullList<ItemStack> stacks) {
        this.stacks = stacks;
        this.journals = new SnapshotJournal[stacks.size()];
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
    public ItemStack insertItem(final int slot, final ItemStack stack, final TransactionContext transaction) {
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

        final int inserted = Math.min(limit, stack.getCount());
        getSlotJournal(slot).updateSnapshots(transaction);
        stacks.set(slot, existing.isEmpty()
            ? stack.copyWithCount(inserted)
            : existing.copyWithCount(existing.getCount() + inserted));

        return inserted < stack.getCount() ? stack.copyWithCount(stack.getCount() - inserted) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final TransactionContext transaction) {
        if (amount <= 0) {
            return ItemStack.EMPTY;
        }

        validateSlot(slot);

        final ItemStack existing = stacks.get(slot);
        if (existing.isEmpty()) {
            return ItemStack.EMPTY;
        }

        final int extracted = MathUtil.min3(amount, existing.getCount(), existing.getMaxStackSize());
        getSlotJournal(slot).updateSnapshots(transaction);
        stacks.set(slot, existing.getCount() > extracted ? existing.copyWithCount(existing.getCount() - extracted) : ItemStack.EMPTY);

        return existing.copyWithCount(extracted);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return Item.DEFAULT_MAX_STACK_SIZE;
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
        journals = new SnapshotJournal[size];
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

    // --------------------------------------------------------------------- //

    private SnapshotJournal getSlotJournal(final int slot) {
        if (journals[slot] == null) {
            journals[slot] = SnapshotJournal.create(() -> stacks.get(slot), stack -> stacks.set(slot, stack), () -> onContentsChanged(slot));
        }
        return journals[slot];
    }

    private void validateSlot(final int slot) {
        if (slot < 0 || slot >= stacks.size()) {
            throw new IndexOutOfBoundsException("Slot " + slot + " not in range [0," + stacks.size() + ")");
        }
    }
}

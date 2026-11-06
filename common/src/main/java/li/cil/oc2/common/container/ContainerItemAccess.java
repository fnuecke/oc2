/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import java.util.function.BiFunction;
import java.util.function.Function;

final class ContainerItemAccess<T> {
    private final ItemHandler inventory;
    private final int slot;
    private final CapabilityType<T> type;

    // --------------------------------------------------------------------- //

    ContainerItemAccess(final ItemHandler inventory, final int slot, final CapabilityType<T> type) {
        this.inventory = inventory;
        this.slot = slot;
        this.type = type;
    }

    // --------------------------------------------------------------------- //

    boolean isPresent() {
        return Capabilities.get(getSingleCopy(), 0, type) != null;
    }

    <R> R read(final Function<T, R> operation, final R fallback) {
        final T capability = Capabilities.get(getSingleCopy(), 0, type);
        return capability != null ? operation.apply(capability) : fallback;
    }

    <R> R modify(final BiFunction<T, TransactionContext, R> operation, final R fallback, final TransactionContext transaction) {
        final ItemStack stack = inventory.getStackInSlot(slot);
        final ItemStackHandler copy = getSingleCopy();
        final T capability = Capabilities.get(copy, 0, type);
        if (capability == null) {
            return fallback;
        }

        try (Transaction nested = transaction.openNested()) {
            final R result = operation.apply(capability, nested);
            final ItemStack modified = copy.getStackInSlot(0);
            if (!ItemStack.matches(modified, stack.copyWithCount(1))) {
                if (stack.getCount() == 1) {
                    // Replace modified instance in-place.
                    if (inventory.extractItem(slot, 1, nested).isEmpty()
                        || !inventory.insertItem(slot, modified, nested).isEmpty()) {
                        return fallback;
                    }
                } else {
                    // Insert modified instance.
                    if (!insertStartingAtSlot(modified, nested).isEmpty()
                        || inventory.extractItem(slot, 1, nested).isEmpty()) {
                        return fallback;
                    }
                }
            }

            nested.commit();
            return result;
        }
    }

    // --------------------------------------------------------------------- //

    private ItemStack insertStartingAtSlot(ItemStack stack, final TransactionContext transaction) {
        for (int i = 0; i < inventory.getSlots() && !stack.isEmpty(); i++) {
            stack = inventory.insertItem((slot + i) % inventory.getSlots(), stack, transaction);
        }
        return stack;
    }

    private ItemStackHandler getSingleCopy() {
        final ItemStackHandler copy = new ItemStackHandler(1);
        copy.setStackInSlot(0, inventory.getStackInSlot(slot).copyWithCount(1));
        return copy;
    }
}

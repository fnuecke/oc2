/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Function;

final class ContainerItemAccess<T> {
    private static final Logger LOGGER = LogManager.getLogger(ContainerItemAccess.class);

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

    <R> R modify(final Function<T, R> operation, final R fallback) {
        final ItemStack stack = inventory.getStackInSlot(slot);
        final ItemStackHandler copy = getSingleCopy();
        final T capability = Capabilities.get(copy, 0, type);
        if (capability == null) {
            return fallback;
        }

        final R result = operation.apply(capability);
        final ItemStack modified = copy.getStackInSlot(0);
        if (ItemStack.matches(modified, stack.copyWithCount(1))) {
            return result;
        }

        if (stack.getCount() == 1) {
            // Replace modified instance in-place.
            if (inventory.extractItem(slot, 1, false).isEmpty()) {
                return fallback;
            }
            if (!inventory.insertItem(slot, modified, false).isEmpty()) {
                // Paranoia, not expected to happen, means there's a bug in the robot inventory.
                LOGGER.error("Inventory [{}] rejected item [{}] in slot [{}] it was just taken from, item was lost.",
                    inventory.getClass().getName(), modified, slot);
            }
        } else {
            // Insert modified instance.
            if (!insertStartingAtSlot(modified).isEmpty()) {
                return fallback;
            }
            if (inventory.extractItem(slot, 1, false).isEmpty()) {
                // Paranoia, not expected to happen, means there's a bug in the robot inventory.
                LOGGER.error("Inventory [{}] did not yield an item from slot [{}] holding [{}], item was duplicated.",
                    inventory.getClass().getName(), slot, stack);
            }
        }

        return result;
    }

    // --------------------------------------------------------------------- //

    private ItemStack insertStartingAtSlot(ItemStack stack) {
        for (int i = 0; i < inventory.getSlots() && !stack.isEmpty(); i++) {
            stack = inventory.insertItem((slot + i) % inventory.getSlots(), stack, false);
        }
        return stack;
    }

    private ItemStackHandler getSingleCopy() {
        final ItemStackHandler copy = new ItemStackHandler(1);
        copy.setStackInSlot(0, inventory.getStackInSlot(slot).copyWithCount(1));
        return copy;
    }
}

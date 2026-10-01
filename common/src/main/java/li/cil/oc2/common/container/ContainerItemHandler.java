/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

public final class ContainerItemHandler implements ItemHandler {
    private final ContainerItemAccess<ItemHandler> access;

    // --------------------------------------------------------------------- //

    private ContainerItemHandler(final ContainerItemAccess<ItemHandler> access) {
        this.access = access;
    }

    @Nullable
    public static ItemHandler of(final ItemHandler inventory, final int slot) {
        final ContainerItemAccess<ItemHandler> access = new ContainerItemAccess<>(inventory, slot, Capabilities.ITEM_HANDLER);
        return access.isPresent() ? new ContainerItemHandler(access) : null;
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getSlots() {
        return access.read(ItemHandler::getSlots, 0);
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        return access.read(handler -> handler.getStackInSlot(slot), ItemStack.EMPTY);
    }

    @Override
    public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
        return simulate
            ? access.read(handler -> handler.insertItem(slot, stack, true), stack)
            : access.modify(handler -> handler.insertItem(slot, stack, false), stack);
    }

    @Override
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        return simulate
            ? access.read(handler -> handler.extractItem(slot, amount, true), ItemStack.EMPTY)
            : access.modify(handler -> handler.extractItem(slot, amount, false), ItemStack.EMPTY);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return access.read(handler -> handler.getSlotLimit(slot), 0);
    }
}

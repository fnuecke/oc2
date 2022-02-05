/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.bus.AbstractItemDeviceBusElement;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;

public abstract class AbstractDeviceItemStackHandler extends FixedSizeItemStackHandler {
    public AbstractDeviceItemStackHandler(final int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public AbstractDeviceItemStackHandler(final NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    ///////////////////////////////////////////////////////////////////

    public abstract AbstractItemDeviceBusElement getBusElement();

    public void exportDeviceDataToItemStacks() {
        for (int slot = 0; slot < getSlots(); slot++) {
            getBusElement().exportDeviceDataToItemStack(slot, getStackInSlot(slot));
        }
    }

    @Override
    public final CompoundTag serializeNBT() {
        throw new UnsupportedOperationException("Use saveItems and saveDevices instead.");
    }

    @Override
    public final void deserializeNBT(final CompoundTag tag) {
        throw new UnsupportedOperationException("Use loadItems and loadDevices instead.");
    }

    public CompoundTag saveItems() {
        return super.serializeNBT();
    }

    public CompoundTag saveDevices() {
        return getBusElement().save();
    }

    public void loadItems(final CompoundTag tag) {
        super.deserializeNBT(tag);
        for (int slot = 0; slot < getSlots(); slot++) {
            getBusElement().updateDevices(slot, getStackInSlot(slot));
        }
    }

    public void loadDevices(final CompoundTag tag) {
        getBusElement().load(tag);
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = super.getStackInSlot(slot);
        getBusElement().exportDeviceDataToItemStack(slot, stack);
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (!simulate && amount > 0) {
            getBusElement().exportDeviceDataToItemStack(slot, super.getStackInSlot(slot));
        }

        return super.extractItem(slot, amount, simulate);
    }

    @Override
    public int getSlotLimit(final int slot) {
        return 1;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected void onContentsChanged(final int slot) {
        super.onContentsChanged(slot);
        getBusElement().updateDevices(slot, getStackInSlot(slot));
    }
}

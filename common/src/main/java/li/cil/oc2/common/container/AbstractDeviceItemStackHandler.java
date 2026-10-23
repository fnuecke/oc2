/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.bus.AbstractItemDeviceBusElement;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class AbstractDeviceItemStackHandler extends FixedSizeItemStackHandler {
    public AbstractDeviceItemStackHandler(final int size) {
        this(NonNullList.withSize(size, ItemStack.EMPTY));
    }

    public AbstractDeviceItemStackHandler(final NonNullList<ItemStack> stacks) {
        super(stacks);
    }

    // --------------------------------------------------------------------- //

    public abstract AbstractItemDeviceBusElement getBusElement();

    public void exportDeviceDataToItemStacks() {
        for (int slot = 0; slot < getSlots(); slot++) {
            getBusElement().exportDeviceDataToItemStack(slot, getStackInSlot(slot));
        }
    }

    @Override
    public final void save(final ValueOutput output) {
        throw new UnsupportedOperationException("Use saveItems and saveDevices instead.");
    }

    @Override
    public final void load(final ValueInput input) {
        throw new UnsupportedOperationException("Use loadItems and loadDevices instead.");
    }

    public void saveItems(final ValueOutput output) {
        exportDeviceDataToItemStacks();

        super.save(output);
    }

    public void saveDevices(final ValueOutput output) {
        getBusElement().save(output);
    }

    public void loadItems(final ValueInput input) {
        super.load(input);
        updateDevices();
    }

    public void updateDevices() {
        for (int slot = 0; slot < getSlots(); slot++) {
            getBusElement().handleSlotContentsChanged(slot, getStackInSlot(slot));
        }
    }

    public void loadDevices(final ValueInput input) {
        getBusElement().load(input);
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = super.getStackInSlot(slot);
        getBusElement().exportDeviceDataToItemStack(slot, stack);
        return stack;
    }

    @Override
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

    // --------------------------------------------------------------------- //

    @Override
    protected void onContentsChanged(final int slot) {
        super.onContentsChanged(slot);
        getBusElement().handleSlotContentsChanged(slot, super.getStackInSlot(slot));
    }
}

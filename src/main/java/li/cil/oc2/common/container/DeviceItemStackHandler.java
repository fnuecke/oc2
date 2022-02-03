/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.ItemHandlerDeviceBusElement;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nonnull;
import java.util.function.Function;

public class DeviceItemStackHandler extends FixedSizeItemStackHandler {
    private final ItemHandlerDeviceBusElement busElement;

    ///////////////////////////////////////////////////////////////////

    public DeviceItemStackHandler(final int size, final Function<ItemStack, ItemDeviceQuery> queryFactory) {
        this(NonNullList.withSize(size, ItemStack.EMPTY), queryFactory);
    }

    public DeviceItemStackHandler(final NonNullList<ItemStack> stacks, final Function<ItemStack, ItemDeviceQuery> queryFactory) {
        super(stacks);
        this.busElement = new ItemHandlerDeviceBusElement(getSlots(), queryFactory);
    }

    ///////////////////////////////////////////////////////////////////

    public ItemHandlerDeviceBusElement getBusElement() {
        return busElement;
    }

    public void exportDeviceDataToItemStacks() {
        for (int slot = 0; slot < getSlots(); slot++) {
            busElement.exportDeviceDataToItemStack(slot, getStackInSlot(slot));
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
        return busElement.save();
    }

    public void loadItems(final CompoundTag tag) {
        super.deserializeNBT(tag);
        for (int slot = 0; slot < getSlots(); slot++) {
            busElement.updateDevices(slot, getStackInSlot(slot));
        }
    }

    public void loadDevices(final CompoundTag tag) {
        busElement.load(tag);
    }

    @Override
    @Nonnull
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = super.getStackInSlot(slot);
        busElement.exportDeviceDataToItemStack(slot, stack);
        return stack;
    }

    @Override
    @Nonnull
    public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
        if (!simulate && amount > 0) {
            busElement.exportDeviceDataToItemStack(slot, super.getStackInSlot(slot));
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
        busElement.updateDevices(slot, getStackInSlot(slot));
    }
}

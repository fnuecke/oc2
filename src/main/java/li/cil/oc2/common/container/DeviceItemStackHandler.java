package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.ItemHandlerDeviceBusElement;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.function.Function;

public class DeviceContainerHelper extends FixedSizeContainerHelper {
    private static final String BUS_ELEMENT_TAG_NAME = "busElement";

    ///////////////////////////////////////////////////////////////////

    private final ItemHandlerDeviceBusElement busElement;

    ///////////////////////////////////////////////////////////////////

    public DeviceContainerHelper(final int size, final Function<ItemStack, ItemDeviceQuery> queryFactory) {
        this(NonNullList.withSize(size, ItemStack.EMPTY), queryFactory);
    }

    public DeviceContainerHelper(final NonNullList<ItemStack> stacks, final Function<ItemStack, ItemDeviceQuery> queryFactory) {
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
    public CompoundTag serializeTag() {
        final CompoundTag tag = super.serializeTag();
        tag.put(BUS_ELEMENT_TAG_NAME, busElement.serializeNBT());
        return tag;
    }

    @Override
    public void deserializeTag(final CompoundTag tag) {
        super.deserializeTag(tag);
        busElement.deserializeNBT(tag.getList(BUS_ELEMENT_TAG_NAME, NBTTagIds.TAG_COMPOUND));
        for (int slot = 0; slot < getSlots(); slot++) {
            busElement.updateDevices(slot, getStackInSlot(slot));
        }
    }

    @Override
    public ItemStack getStackInSlot(final int slot) {
        final ItemStack stack = super.getStackInSlot(slot);
        busElement.exportDeviceDataToItemStack(slot, stack);
        return stack;
    }

    @Override
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

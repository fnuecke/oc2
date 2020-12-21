package li.cil.oc2.common.container;

import alexiil.mc.lib.attributes.Simulation;
import alexiil.mc.lib.attributes.item.FixedItemInvView;
import alexiil.mc.lib.attributes.item.filter.ItemFilter;
import alexiil.mc.lib.attributes.item.impl.FullFixedItemInv;
import li.cil.oc2.api.bus.DeviceBusElement;
import li.cil.oc2.common.bus.ItemHandlerDeviceBusElement;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public class DeviceItemStackHandler extends FullFixedItemInv {
    private static final String BUS_ELEMENT_NBT_TAG_NAME = "busElement";

    ///////////////////////////////////////////////////////////////////

    private final ItemHandlerDeviceBusElement busElement;

    ///////////////////////////////////////////////////////////////////

    public DeviceItemStackHandler(final int size) {
        super(size);
        this.busElement = new ItemHandlerDeviceBusElement(getSlotCount());
        addListener(this::onContentsChanged, () -> {
        });
    }

    ///////////////////////////////////////////////////////////////////

    public DeviceBusElement getBusElement() {
        return busElement;
    }

    @Override
    public CompoundTag toTag(CompoundTag nbt) {
        nbt = super.toTag(nbt);
        nbt.put(BUS_ELEMENT_NBT_TAG_NAME, busElement.serializeNBT());
        return nbt;
    }

    @Override
    public void fromTag(final CompoundTag nbt) {
        super.fromTag(nbt);
        busElement.deserializeNBT(nbt.getList(BUS_ELEMENT_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND));
        for (int slot = 0; slot < getSlotCount(); slot++) {
            busElement.updateDevices(slot, getInvStack(slot));
        }
    }

    @Override
    public ItemStack extractStack(final int slot, @Nullable final ItemFilter filter, final ItemStack mergeWith, final int amount, final Simulation simulation) {
        if (simulation.isAction() && amount > 0) {
            busElement.handleBeforeItemRemoved(slot, getInvStack(slot));
        }

        return super.extractStack(slot, filter, mergeWith, amount, simulation);
    }

    @Override
    public int getMaxAmount(final int slot, final ItemStack stack) {
        return 1;
    }

//    @Override
//    public boolean isItemValidForSlot(final int slot, final ItemStack item) {
//        return super.isItemValidForSlot(slot, item);
//    }

    ///////////////////////////////////////////////////////////////////

    private void onContentsChanged(final FixedItemInvView unused, final int slot, final ItemStack previous, final ItemStack current) {
        busElement.updateDevices(slot, current);
    }
}

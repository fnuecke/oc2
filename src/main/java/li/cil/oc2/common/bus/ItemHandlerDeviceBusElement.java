package li.cil.oc2.common.bus;

import li.cil.oc2.common.bus.device.Devices;
import li.cil.oc2.common.bus.device.ItemDeviceInfo;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundTag;

import java.util.Collections;
import java.util.HashSet;

public class ItemHandlerDeviceBusElement extends AbstractGroupingItemDeviceBusElement {
    public ItemHandlerDeviceBusElement(final int slotCount) {
        super(slotCount);
    }

    ///////////////////////////////////////////////////////////////////

    public void updateDevices(final int slot, final ItemStack stack) {
        if (!stack.isEmpty()) {
            final HashSet<ItemDeviceInfo> newDevices = new HashSet<>(Devices.getDevices(stack));
            importDeviceDataFromItemStack(stack, newDevices);
            setDevicesForGroup(slot, newDevices);
        } else {
            setDevicesForGroup(slot, Collections.emptySet());
        }
    }

    public void handleBeforeItemRemoved(final int slot, final ItemStack stack) {
        if (!stack.isEmpty()) {
            exportDeviceDataToItemStack(slot, stack);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void exportDeviceDataToItemStack(final int slot, final ItemStack stack) {
        final CompoundTag exportedNbt = new CompoundTag();
        for (final ItemDeviceInfo info : groups.get(slot)) {
            ItemDeviceUtils.getItemDeviceDataKey(info.registry, info.provider).ifPresent(key -> {
                final CompoundTag deviceNbt = new CompoundTag();
                info.device.exportToItemStack(deviceNbt);
                if (!deviceNbt.isEmpty()) {
                    exportedNbt.put(key, deviceNbt);
                }
            });
        }

        ItemDeviceUtils.setItemDeviceData(stack, exportedNbt);
    }

    private void importDeviceDataFromItemStack(final ItemStack stack, final HashSet<ItemDeviceInfo> devices) {
        ItemDeviceUtils.getItemDeviceData(stack).ifPresent(exportedNbt -> {
            for (final ItemDeviceInfo info : devices) {
                ItemDeviceUtils.getItemDeviceDataKey(info.registry, info.provider).ifPresent(key -> {
                    if (exportedNbt.contains(key, NBTTagIds.TAG_COMPOUND)) {
                        info.device.importFromItemStack(exportedNbt.getCompound(key));
                    }
                });
            }
        });
    }
}

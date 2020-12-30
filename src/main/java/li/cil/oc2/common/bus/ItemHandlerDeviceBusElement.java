package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ResourceLocation;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

public class ItemHandlerDeviceBusElement extends AbstractGroupingItemDeviceBusElement {
    private final Function<ItemStack, List<ItemDeviceInfo>> deviceLookup;

    public ItemHandlerDeviceBusElement(final int slotCount, final Function<ItemStack, List<ItemDeviceInfo>> deviceLookup) {
        super(slotCount);
        this.deviceLookup = deviceLookup;
    }

    ///////////////////////////////////////////////////////////////////

    public void updateDevices(final int slot, final ItemStack stack) {
        if (!stack.isEmpty()) {
            final HashSet<ItemDeviceInfo> newDevices = new HashSet<>(deviceLookup.apply(stack));
            insertItemNameDevice(stack, newDevices);
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
        final CompoundNBT exportedNbt = new CompoundNBT();
        for (final ItemDeviceInfo info : groups.get(slot)) {
            ItemDeviceUtils.getItemDeviceDataKey(info.provider).ifPresent(key -> {
                final CompoundNBT deviceNbt = new CompoundNBT();
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
                ItemDeviceUtils.getItemDeviceDataKey(info.provider).ifPresent(key -> {
                    if (exportedNbt.contains(key, NBTTagIds.TAG_COMPOUND)) {
                        info.device.importFromItemStack(exportedNbt.getCompound(key));
                    }
                });
            }
        });
    }

    private void insertItemNameDevice(final ItemStack stack, final HashSet<ItemDeviceInfo> devices) {
        if (devices.stream().anyMatch(info -> info.device instanceof RPCDevice)) {
            final ResourceLocation registryName = stack.getItem().getRegistryName();
            if (registryName != null) {
                devices.add(new ItemDeviceInfo(null, new TypeNameRPCDevice(registryName.toString())));
            }
        }
    }
}

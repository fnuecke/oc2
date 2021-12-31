package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.HashSet;
import java.util.function.Function;

public final class ItemHandlerDeviceBusElement extends AbstractGroupingItemDeviceBusElement {
    private final Function<ItemStack, ItemDeviceQuery> queryFactory;

    public ItemHandlerDeviceBusElement(final int slotCount, final Function<ItemStack, ItemDeviceQuery> queryFactory) {
        super(slotCount);
        this.queryFactory = queryFactory;
    }

    ///////////////////////////////////////////////////////////////////

    public void updateDevices(final int slot, final ItemStack stack) {
        if (!stack.isEmpty()) {
            final ItemDeviceQuery query = queryFactory.apply(stack);
            final HashSet<ItemDeviceInfo> newDevices = new HashSet<>(Devices.getDevices(query));
            insertItemNameDevice(stack, newDevices);
            importDeviceDataFromItemStack(stack, newDevices);
            setDevicesForGroup(slot, newDevices);
        } else {
            setDevicesForGroup(slot, Collections.emptySet());
        }
    }

    public void exportDeviceDataToItemStack(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        final CompoundTag exportedTag = new CompoundTag();
        for (final ItemDeviceInfo info : groups.get(slot)) {
            ItemDeviceUtils.getItemDeviceDataKey(info.provider).ifPresent(key -> {
                final CompoundTag deviceTag = new CompoundTag();
                info.device.exportToItemStack(deviceTag);
                if (!deviceTag.isEmpty()) {
                    exportedTag.put(key, deviceTag);
                }
            });
        }

        if (!exportedTag.isEmpty()) {
            ItemDeviceUtils.setItemDeviceData(stack, exportedTag);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void importDeviceDataFromItemStack(final ItemStack stack, final HashSet<ItemDeviceInfo> devices) {
        final CompoundTag exportedTag = ItemDeviceUtils.getItemDeviceData(stack);
        if (!exportedTag.isEmpty()) {
            for (final ItemDeviceInfo info : devices) {
                ItemDeviceUtils.getItemDeviceDataKey(info.provider).ifPresent(key -> {
                    if (exportedTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                        info.device.importFromItemStack(exportedTag.getCompound(key));
                    }
                });
            }
        }
    }

    private void insertItemNameDevice(final ItemStack stack, final HashSet<ItemDeviceInfo> devices) {
        if (devices.stream().anyMatch(info -> info.device instanceof RPCDevice)) {
            final ResourceLocation registryName = stack.getItem().getRegistryName();
            if (registryName != null) {
                devices.add(new ItemDeviceInfo(null, new TypeNameRPCDevice(registryName.toString()), 0));
            }
        }
    }
}

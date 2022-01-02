package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
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

import java.util.*;
import java.util.function.Function;

import static li.cil.oc2.common.util.RegistryUtils.optionalKey;

public final class ItemHandlerDeviceBusElement extends AbstractGroupingDeviceBusElement<ItemHandlerDeviceBusElement.ItemEntry> {
    private final Function<ItemStack, ItemDeviceQuery> queryFactory;

    public ItemHandlerDeviceBusElement(final int slotCount, final Function<ItemStack, ItemDeviceQuery> queryFactory) {
        super(slotCount);
        this.queryFactory = queryFactory;
    }

    ///////////////////////////////////////////////////////////////////

    public boolean groupContains(final int groupIndex, final Device device) {
        for (final ItemEntry entry : groups.get(groupIndex)) {
            if (Objects.equals(entry.getDevice(), device)) {
                return true;
            }
        }

        return false;
    }

    public void updateDevices(final int slot, final ItemStack stack) {
        if (!stack.isEmpty()) {
            final ItemDeviceQuery query = queryFactory.apply(stack);
            final HashSet<ItemEntry> newDevices = new HashSet<>(Devices.getDevices(query).stream().map(ItemEntry::new).toList());
            insertItemNameDevice(stack, newDevices);
            importDeviceDataFromItemStack(stack, newDevices);
            setEntriesForGroup(slot, newDevices);
        } else {
            setEntriesForGroup(slot, Collections.emptySet());
        }
    }

    public void exportDeviceDataToItemStack(final int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        final CompoundTag exportedTag = new CompoundTag();
        for (final ItemEntry entry : groups.get(slot)) {
            entry.getDeviceDataKey().ifPresent(key -> {
                final CompoundTag deviceTag = new CompoundTag();
                entry.getDevice().exportToItemStack(deviceTag);
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

    private void importDeviceDataFromItemStack(final ItemStack stack, final HashSet<ItemEntry> entries) {
        final CompoundTag exportedTag = ItemDeviceUtils.getItemDeviceData(stack);
        if (!exportedTag.isEmpty()) {
            for (final ItemEntry entry : entries) {
                entry.getDeviceDataKey().ifPresent(key -> {
                    if (exportedTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                        entry.deviceInfo.device.importFromItemStack(exportedTag.getCompound(key));
                    }
                });
            }
        }
    }

    private void insertItemNameDevice(final ItemStack stack, final HashSet<ItemEntry> entries) {
        if (entries.stream().anyMatch(entry -> entry.getDevice() instanceof RPCDevice)) {
            final ResourceLocation registryName = stack.getItem().getRegistryName();
            if (registryName != null) {
                entries.add(new ItemEntry(new ItemDeviceInfo(null, new TypeNameRPCDevice(registryName.toString()), 0)));
            }
        }
    }

    ///////////////////////////////////////////////////////////////////

    protected record ItemEntry(ItemDeviceInfo deviceInfo) implements Entry {
        @Override
        public Optional<String> getDeviceDataKey() {
            return optionalKey(deviceInfo.provider);
        }

        @Override
        public OptionalInt getDeviceEnergyConsumption() {
            return OptionalInt.of(deviceInfo.getEnergyConsumption());
        }

        @Override
        public ItemDevice getDevice() {
            return deviceInfo.device;
        }
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.common.bus.device.provider.Providers;
import li.cil.oc2.common.bus.device.rpc.TypeNameRPCDevice;
import li.cil.oc2.common.bus.device.util.Devices;
import li.cil.oc2.common.bus.device.util.ItemDeviceInfo;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.*;

import static li.cil.oc2.common.util.RegistryUtils.optionalKey;

public abstract class AbstractItemDeviceBusElement extends AbstractGroupingDeviceBusElement<AbstractItemDeviceBusElement.ItemEntry, ItemDeviceQuery> {
    public AbstractItemDeviceBusElement(final int groupCount) {
        super(groupCount);
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
            final ItemDeviceQuery query = makeQuery(stack);
            final HashSet<ItemEntry> newDevices = new HashSet<>(Devices.getDevices(query).stream().map(ItemEntry::new).toList());
            insertItemNameDevice(stack, newDevices);
            importDeviceDataFromItemStack(stack, newDevices);
            setEntriesForGroup(slot, new ItemQueryResult(query, newDevices));
        } else {
            setEntriesForGroup(slot, new ItemQueryResult(null, Collections.emptySet()));
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

    protected abstract ItemDeviceQuery makeQuery(final ItemStack stack);

    @Override
    protected void onEntryRemoved(final String dataKey, final CompoundTag tag, @Nullable final ItemDeviceQuery query) {
        super.onEntryRemoved(dataKey, tag, query);
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY.get();
        final ItemDeviceProvider provider = registry.getValue(new ResourceLocation(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
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

    protected final class ItemQueryResult extends QueryResult {
        @Nullable private final ItemDeviceQuery query;
        private final Set<ItemEntry> entries;

        public ItemQueryResult(@Nullable final ItemDeviceQuery query, final Set<ItemEntry> entries) {
            this.query = query;
            this.entries = entries;
        }

        @Nullable
        @Override
        public ItemDeviceQuery getQuery() {
            return query;
        }

        @Override
        public Set<ItemEntry> getEntries() {
            return entries;
        }
    }

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

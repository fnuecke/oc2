/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
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

    public void handleSlotContentsChanged(final int slot, final ItemStack stack) {
        final ItemQueryResult queryResult = collectDevices(stack);

        setEntriesForGroup(slot, queryResult);
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

    protected ItemQueryResult collectDevices(final ItemStack stack) {
        final ItemDeviceQuery query = makeQuery(stack);
        final HashSet<ItemEntry> entries = new HashSet<>();

        for (final ItemDeviceInfo deviceInfo : Devices.getDevices(query)) {
            entries.add(new ItemEntry(deviceInfo));
        }

        collectSyntheticDevices(query, entries);

        importDeviceDataFromItemStack(query, entries);

        return new ItemQueryResult(query, entries);
    }

    protected void collectSyntheticDevices(final ItemDeviceQuery query, final HashSet<ItemEntry> entries) {
        if (entries.isEmpty()) {
            return;
        }

        final ResourceLocation registryName = query.getItemStack().getItem().getRegistryName();
        if (registryName != null) {
            final String itemName = registryName.toString();
            entries.add(new ItemEntry(new ItemDeviceInfo(null, new TypeNameRPCDevice(itemName), 0)));
        }
    }

    @Override
    protected void onEntryRemoved(final String dataKey, final CompoundTag tag, @Nullable final ItemDeviceQuery query) {
        super.onEntryRemoved(dataKey, tag, query);
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.itemDeviceProviderRegistry();
        final ItemDeviceProvider provider = registry.getValue(new ResourceLocation(dataKey));
        if (provider != null) {
            provider.unmount(query, tag);
        }
    }

    ///////////////////////////////////////////////////////////////////

    private void importDeviceDataFromItemStack(final ItemDeviceQuery query, final HashSet<ItemEntry> entries) {
        final CompoundTag exportedTag = ItemDeviceUtils.getItemDeviceData(query.getItemStack());
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

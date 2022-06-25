/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import javax.annotation.Nullable;
import java.util.*;

public abstract class AbstractGroupingDeviceBusElement<TEntry extends AbstractGroupingDeviceBusElement.Entry, TQuery> extends AbstractDeviceBusElement {
    private static final String GROUPS_TAG_NAME = "groups";
    private static final String GROUP_ID_TAG_NAME = "groupId";
    private static final String GROUP_DATA_TAG_NAME = "groupData";

    protected abstract class QueryResult {
        @Nullable
        public abstract TQuery getQuery();

        public abstract Set<TEntry> getEntries();
    }

    protected interface Entry {
        Optional<String> getDeviceDataKey();

        OptionalInt getDeviceEnergyConsumption();

        Device getDevice();
    }

    ///////////////////////////////////////////////////////////////////

    protected final int groupCount;
    protected final ArrayList<HashSet<TEntry>> groups;

    ///////////////////////////////////////////////////////////////////

    protected final UUID[] groupIds;
    protected final CompoundTag[] groupData;

    ///////////////////////////////////////////////////////////////////

    protected AbstractGroupingDeviceBusElement(final int groupCount) {
        this.groupCount = groupCount;
        this.groups = new ArrayList<>(groupCount);
        this.groupIds = new UUID[groupCount];
        this.groupData = new CompoundTag[groupCount];

        for (int i = 0; i < groupCount; i++) {
            groups.add(new HashSet<>());
            groupIds[i] = UUID.randomUUID();
            groupData[i] = new CompoundTag();
        }
    }

    ///////////////////////////////////////////////////////////////////

    public CompoundTag save() {
        final ListTag listTag = new ListTag();
        for (int i = 0; i < groupCount; i++) {
            saveGroup(i);

            final CompoundTag sideTag = new CompoundTag();

            sideTag.putUUID(GROUP_ID_TAG_NAME, groupIds[i]);
            sideTag.put(GROUP_DATA_TAG_NAME, groupData[i]);

            listTag.add(sideTag);
        }

        final CompoundTag tag = new CompoundTag();
        tag.put(GROUPS_TAG_NAME, listTag);
        return tag;
    }

    public void load(final CompoundTag tag) {
        final ListTag listTag = tag.getList(GROUPS_TAG_NAME, NBTTagIds.TAG_COMPOUND);

        final int count = Math.min(groupCount, listTag.size());
        for (int i = 0; i < count; i++) {
            final CompoundTag sideTag = listTag.getCompound(i);

            if (sideTag.hasUUID(GROUP_ID_TAG_NAME)) {
                groupIds[i] = sideTag.getUUID(GROUP_ID_TAG_NAME);
            }
            if (sideTag.contains(GROUP_DATA_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                groupData[i] = sideTag.getCompound(GROUP_DATA_TAG_NAME);
            }

            // Immediately load data into devices, if we already have some.
            for (final TEntry entry : groups.get(i)) {
                final CompoundTag devicesTag = groupData[i];
                entry.getDeviceDataKey().ifPresent(key -> {
                    if (devicesTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                        entry.getDevice().deserializeNBT(devicesTag.getCompound(key));
                    }
                });
            }
        }
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        for (int i = 0; i < groupCount; i++) {
            final HashSet<TEntry> group = groups.get(i);
            for (final TEntry deviceInfo : group) {
                if (Objects.equals(device, deviceInfo.getDevice())) {
                    return Optional.of(groupIds[i]);
                }
            }
        }
        return super.getDeviceIdentifier(device);
    }

    ///////////////////////////////////////////////////////////////////

    protected final void setEntriesForGroupUnloaded(final int index) {
        final HashSet<TEntry> oldEntries = groups.get(index);
        if (oldEntries.isEmpty()) {
            return;
        }

        saveGroup(index);

        for (final TEntry entry : oldEntries) {
            devices.removeInt(entry.getDevice());
            onEntryRemoved(entry);
        }

        oldEntries.clear();

        scanDevices();
    }

    protected final void setEntriesForGroup(final int index, final QueryResult queryResult) {
        final Set<TEntry> newEntries = queryResult.getEntries();
        final HashSet<TEntry> entries = groups.get(index);
        if (Objects.equals(newEntries, entries)) {
            if (entries.isEmpty()) {
                // If we do not have any entries, we still need to check if there's any
                // remaining data of previously known devices, so we can call dispose
                // on the appropriate provider. If we don't do this here, we may delay
                // this indefinitely, if no new devices are detected for this index.
                final CompoundTag devicesTag = groupData[index];
                if (!devicesTag.isEmpty()) {
                    final Iterator<String> iterator = devicesTag.getAllKeys().iterator();
                    while (iterator.hasNext()) {
                        final String dataKey = iterator.next();
                        if (devicesTag.contains(dataKey, NBTTagIds.TAG_COMPOUND)) {
                            final CompoundTag tag = devicesTag.getCompound(dataKey);
                            onEntryRemoved(dataKey, tag, queryResult.getQuery());
                        }
                        iterator.remove();
                    }
                }
            }

            return;
        }

        final boolean hadOldEntries = !entries.isEmpty();

        final HashSet<TEntry> removedEntries = new HashSet<>(entries);
        removedEntries.removeAll(newEntries);
        for (final TEntry entry : removedEntries) {
            devices.removeInt(entry.getDevice());
            onEntryRemoved(entry);
        }

        final HashSet<TEntry> addedEntries = new HashSet<>(newEntries);
        addedEntries.removeAll(entries);
        for (final TEntry entry : addedEntries) {
            devices.put(entry.getDevice(), entry.getDeviceEnergyConsumption().orElse(0));
            onEntryAdded(entry);
        }

        entries.removeAll(removedEntries);
        entries.addAll(newEntries);

        // Remove serialized data for devices that were present before, but are gone now.
        final CompoundTag devicesTag = groupData[index];
        for (final TEntry entry : removedEntries) {
            entry.getDeviceDataKey().ifPresent(devicesTag::remove);
        }

        // Deserialize data for found devices, if we have existing data for them. Also collect
        // the list of serialized data we have for devices that have gone missing without being
        // explicitly removed. This can happen if a device is removed while the bus element is
        // unloaded. We need to call dispose on the provider if we detect this.
        final HashSet<String> invalidDataKeys = new HashSet<>(devicesTag.getAllKeys());
        for (final TEntry entry : addedEntries) {
            entry.getDeviceDataKey().ifPresent(key -> {
                invalidDataKeys.remove(key);
                if (devicesTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                    entry.getDevice().deserializeNBT(devicesTag.getCompound(key));
                } else {
                    devicesTag.remove(key);
                }
            });
        }

        final TQuery query = queryResult.getQuery();
        for (final String invalidDataKey : invalidDataKeys) {
            if (devicesTag.contains(invalidDataKey, NBTTagIds.TAG_COMPOUND)) {
                final CompoundTag tag = devicesTag.getCompound(invalidDataKey);
                onEntryRemoved(invalidDataKey, tag, query);
            }
            devicesTag.remove(invalidDataKey);
        }

        // Assign a new ID to this side when the device configuration changes. Avoids confusion when
        // providing a different device to an interface when some running use programs in the VM may
        // still hold a reference to the old one. We consider "changing" anything that removes some
        // devices or adds new devices to an *existing* device set.
        if (hadOldEntries) {
            groupIds[index] = UUID.randomUUID();
        }

        // Trigger a device update on the controller, if we have one. This gives adapters a chance
        // to unmount old devices before we call dispose on them in the next step. If we don't
        // have a controller, devices must already be unmounted (either not mounted in this lifetime,
        // or controller unmounted them when it was removed).
        scanDevices();

        for (final TEntry entry : removedEntries) {
            entry.getDevice().dispose();
        }
    }

    protected void onEntryAdded(final TEntry entry) {
    }

    protected void onEntryRemoved(final TEntry entry) {
    }

    protected void onEntryRemoved(final String dataKey, final CompoundTag data, @Nullable final TQuery query) {
    }

    ///////////////////////////////////////////////////////////////////

    private void saveGroup(final int index) {
        final CompoundTag devicesTag = new CompoundTag();
        for (final TEntry entry : groups.get(index)) {
            entry.getDeviceDataKey().ifPresent(key -> {
                // Always store, even if the data is empty, so we know an device by this provider existed.
                devicesTag.put(key, entry.getDevice().serializeNBT());
            });
        }

        groupData[index] = devicesTag;
    }
}

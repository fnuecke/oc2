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

    protected final void setEntriesForGroup(final int index, final QueryResult queryResult) {
        final Set<TEntry> newEntries = queryResult.getEntries();
        final HashSet<TEntry> oldEntries = groups.get(index);
        if (Objects.equals(newEntries, oldEntries)) {
            return;
        }

        final HashSet<TEntry> removedEntries = new HashSet<>(oldEntries);
        removedEntries.removeAll(newEntries);
        for (final TEntry entry : removedEntries) {
            devices.removeInt(entry.getDevice());
            onEntryRemoved(entry);
        }

        final HashSet<TEntry> addedEntries = new HashSet<>(newEntries);
        addedEntries.removeAll(oldEntries);
        for (final TEntry entry : addedEntries) {
            devices.put(entry.getDevice(), entry.getDeviceEnergyConsumption().orElse(0));
            onEntryAdded(entry);
        }

        oldEntries.removeAll(removedEntries);
        oldEntries.addAll(newEntries);

        final CompoundTag devicesTag = groupData[index];
        for (final TEntry entry : removedEntries) {
            entry.getDeviceDataKey().ifPresent(devicesTag::remove);
        }

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

        scanDevices();
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
                final CompoundTag deviceTag = entry.getDevice().serializeNBT();
                if (!deviceTag.isEmpty()) {
                    devicesTag.put(key, deviceTag);
                }
            });
        }

        groupData[index] = devicesTag;
    }
}

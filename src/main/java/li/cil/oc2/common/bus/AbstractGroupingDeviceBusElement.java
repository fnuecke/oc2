package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;

import java.util.*;

public abstract class AbstractGroupingDeviceBusElement<T extends AbstractGroupingDeviceBusElement.Entry> extends AbstractDeviceBusElement {
    private static final String GROUPS_TAG_NAME = "groups";
    private static final String GROUP_ID_TAG_NAME = "groupId";
    private static final String GROUP_DATA_TAG_NAME = "groupData";

    protected interface Entry {
        Optional<String> getDeviceDataKey();

        OptionalInt getDeviceEnergyConsumption();

        Device getDevice();
    }

    ///////////////////////////////////////////////////////////////////

    protected final int groupCount;
    protected final ArrayList<HashSet<T>> groups;

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
        }
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        for (int i = 0; i < groupCount; i++) {
            final HashSet<T> group = groups.get(i);
            for (final T deviceInfo : group) {
                if (Objects.equals(device, deviceInfo.getDevice())) {
                    return Optional.of(groupIds[i]);
                }
            }
        }
        return super.getDeviceIdentifier(device);
    }

    ///////////////////////////////////////////////////////////////////

    protected final void setEntriesForGroup(final int index, final Set<T> newEntries) {
        final HashSet<T> oldEntries = groups.get(index);
        if (Objects.equals(newEntries, oldEntries)) {
            return;
        }

        final HashSet<T> removedEntries = new HashSet<>(oldEntries);
        removedEntries.removeAll(newEntries);
        for (final T entry : removedEntries) {
            devices.removeInt(entry.getDevice());
            onEntryRemoved(entry);
        }

        final HashSet<T> addedEntries = new HashSet<>(newEntries);
        addedEntries.removeAll(oldEntries);
        for (final T entry : addedEntries) {
            devices.put(entry.getDevice(), entry.getDeviceEnergyConsumption().orElse(0));
            onEntryAdded(entry);
        }

        oldEntries.removeAll(removedEntries);
        oldEntries.addAll(newEntries);

        final CompoundTag devicesTag = groupData[index];
        for (final T entry : removedEntries) {
            entry.getDeviceDataKey().ifPresent(devicesTag::remove);
        }
        for (final T entry : addedEntries) {
            entry.getDeviceDataKey().ifPresent(key -> {
                if (devicesTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                    entry.getDevice().deserializeNBT(devicesTag.getCompound(key));
                }
            });
        }

        scanDevices();
    }

    protected void onEntryRemoved(final T entry) {
    }

    protected void onEntryAdded(final T entry) {
    }

    ///////////////////////////////////////////////////////////////////

    private void saveGroup(final int index) {
        final CompoundTag devicesTag = new CompoundTag();
        for (final T entry : groups.get(index)) {
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

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus;

import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import li.cil.oc2.api.bus.DeviceBusController;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.SerializationUtils;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.*;

public abstract class AbstractGroupingDeviceBusElement<TEntry extends AbstractGroupingDeviceBusElement.Entry> extends AbstractDeviceBusElement {
    private static final String GROUPS_TAG_NAME = "groups";
    private static final String GROUP_ID_TAG_NAME = "groupId";
    private static final String GROUP_DATA_TAG_NAME = "groupData";

    protected interface Entry {
        Optional<String> getDeviceDataKey();

        OptionalInt getDeviceEnergyConsumption();

        Device getDevice();
    }

    // --------------------------------------------------------------------- //

    protected final int groupCount;
    protected final ArrayList<HashSet<TEntry>> groups;
    private final Object2IntOpenHashMap<Device> deviceGroups = new Object2IntOpenHashMap<>();

    // --------------------------------------------------------------------- //

    protected final UUID[] groupIds;
    protected final CompoundTag[] groupData;

    // --------------------------------------------------------------------- //

    protected AbstractGroupingDeviceBusElement(final int groupCount) {
        this.groupCount = groupCount;
        this.groups = new ArrayList<>(groupCount);
        this.groupIds = new UUID[groupCount];
        this.groupData = new CompoundTag[groupCount];

        deviceGroups.defaultReturnValue(-1);

        for (int i = 0; i < groupCount; i++) {
            groups.add(new HashSet<>());
            groupIds[i] = UUID.randomUUID();
            groupData[i] = new CompoundTag();
        }
    }

    // --------------------------------------------------------------------- //

    public void save(final ValueOutput output) {
        if (groups.stream().anyMatch(group -> !group.isEmpty())) {
            onSaving();
        }

        final ValueOutput.ValueOutputList groupsOutput = output.childrenList(GROUPS_TAG_NAME);
        for (int i = 0; i < groupCount; i++) {
            final ValueOutput groupOutput = groupsOutput.addChild();
            groupOutput.store(GROUP_ID_TAG_NAME, UUIDUtil.CODEC, groupIds[i]);
            saveGroup(i, groupOutput.child(GROUP_DATA_TAG_NAME));
        }
    }

    public void load(final ValueInput input) {
        int i = 0;
        for (final ValueInput groupInput : input.childrenListOrEmpty(GROUPS_TAG_NAME)) {
            if (i >= groupCount) {
                break;
            }

            groupIds[i] = groupInput.read(GROUP_ID_TAG_NAME, UUIDUtil.CODEC).orElse(groupIds[i]);
            groupData[i] = groupInput.read(GROUP_DATA_TAG_NAME, CompoundTag.CODEC).orElse(groupData[i]);

            // Immediately load data into devices, if we already have some.
            final ValueInput devicesInput = groupInput.childOrEmpty(GROUP_DATA_TAG_NAME);
            for (final TEntry entry : groups.get(i)) {
                entry.getDeviceDataKey()
                    .flatMap(devicesInput::child)
                    .ifPresent(entry.getDevice()::deserialize);
            }

            i++;
        }
    }

    public void reset() {
        final boolean hadEntries = groups.stream().anyMatch(group -> !group.isEmpty());
        for (int i = 0; i < groupCount; i++) {
            for (final TEntry entry : groups.get(i)) {
                devices.removeInt(entry.getDevice());
                deviceGroups.removeInt(entry.getDevice());
                onEntryRemoved(entry);
            }
            groups.get(i).clear();
            groupData[i] = new CompoundTag();
        }

        if (hadEntries) {
            scanDevices();
        }
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        final int index = deviceGroups.getInt(device);
        if (index >= 0) {
            return Optional.of(groupIds[index]);
        }
        return super.getDeviceIdentifier(device);
    }

    // --------------------------------------------------------------------- //

    protected final void setEntriesForGroupUnloaded(final int index) {
        final HashSet<TEntry> oldEntries = groups.get(index);
        if (oldEntries.isEmpty()) {
            return;
        }

        onSaving();
        final CompoundTag devicesTag = groupData[index];
        for (final TEntry entry : oldEntries) {
            entry.getDeviceDataKey().ifPresent(key ->
                devicesTag.put(key, SerializationUtils.saveToTag(new ProblemReporter.RootFieldPathElement(key), getRegistries(), entry.getDevice()::serialize)));
        }

        for (final TEntry entry : oldEntries) {
            devices.removeInt(entry.getDevice());
            deviceGroups.removeInt(entry.getDevice());
            onEntryRemoved(entry);
        }

        oldEntries.clear();

        scanDevices();
    }

    protected final void setEntriesForGroup(final int index, final Set<TEntry> newEntries) {
        final HashSet<TEntry> entries = groups.get(index);
        if (Objects.equals(newEntries, entries)) {
            if (entries.isEmpty()) {
                groupData[index].keySet().clear();
            }

            return;
        }

        final boolean hadOldEntries = !entries.isEmpty();

        final HashSet<TEntry> removedEntries = new HashSet<>(entries);
        removedEntries.removeAll(newEntries);
        for (final TEntry entry : removedEntries) {
            devices.removeInt(entry.getDevice());
            deviceGroups.removeInt(entry.getDevice());
            onEntryRemoved(entry);
        }

        final HashSet<TEntry> addedEntries = new HashSet<>(newEntries);
        addedEntries.removeAll(entries);
        for (final TEntry entry : addedEntries) {
            devices.put(entry.getDevice(), entry.getDeviceEnergyConsumption().orElse(0));
            deviceGroups.put(entry.getDevice(), index);
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
        // unloaded.
        final HashSet<String> invalidDataKeys = new HashSet<>(devicesTag.keySet());
        for (final TEntry entry : addedEntries) {
            entry.getDeviceDataKey().ifPresent(key -> {
                invalidDataKeys.remove(key);
                final Optional<CompoundTag> deviceTag = devicesTag.getCompound(key);
                if (deviceTag.isPresent()) {
                    SerializationUtils.loadFromTag(new ProblemReporter.RootFieldPathElement(key), getRegistries(), deviceTag.get(), entry.getDevice()::deserialize);
                } else {
                    devicesTag.remove(key);
                }
            });
        }

        invalidDataKeys.forEach(devicesTag::remove);

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

    protected abstract HolderLookup.Provider getRegistries();

    protected void onEntryAdded(final TEntry entry) {
    }

    protected void onEntryRemoved(final TEntry entry) {
    }

    // --------------------------------------------------------------------- //

    private void onSaving() {
        for (final DeviceBusController controller : controllers) {
            if (controller instanceof final CommonDeviceBusController commonController) {
                commonController.onSaving();
            }
        }
    }

    private void saveGroup(final int index, final ValueOutput output) {
        // We keep data of devices that are not present (yet), because we might be saving before we
        // had a chance to do a full scan (something triggered a chunk save immediately after its load
        // for example). Otherwise, we might end up dropping saved data here, which would then be missing
        // on the next load+scan. Any stale data we accumulate this way will be cleaned up in the next
        // controller scan.
        final CompoundTag devicesTag = groupData[index];
        final HashSet<String> savedKeys = new HashSet<>();
        for (final TEntry entry : groups.get(index)) {
            entry.getDeviceDataKey().ifPresent(key -> {
                // Always store, even if the data is empty, so we know a device by this provider existed.
                entry.getDevice().serialize(output.child(key));
                savedKeys.add(key);
            });
        }
        for (final String key : devicesTag.keySet()) {
            if (!savedKeys.contains(key)) {
                devicesTag.getCompound(key).ifPresent(tag -> output.store(key, CompoundTag.CODEC, tag));
            }
        }
    }
}

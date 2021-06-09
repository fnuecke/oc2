package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.util.AbstractDeviceInfo;
import li.cil.oc2.common.util.ItemDeviceUtils;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.registries.IForgeRegistryEntry;

import java.util.*;

public abstract class AbstractGroupingDeviceBusElement<TProvider extends IForgeRegistryEntry<TProvider>, TDeviceInfo extends AbstractDeviceInfo<TProvider, ?>> extends AbstractDeviceBusElement implements INBTSerializable<ListNBT> {
    private static final String GROUP_ID_TAG_NAME = "groupId";
    private static final String GROUP_DATA_TAG_NAME = "groupData";

    ///////////////////////////////////////////////////////////////////

    protected final int groupCount;
    protected final ArrayList<HashSet<TDeviceInfo>> groups;

    ///////////////////////////////////////////////////////////////////

    protected final UUID[] groupIds;
    protected final CompoundNBT[] groupData;

    ///////////////////////////////////////////////////////////////////

    protected AbstractGroupingDeviceBusElement(final int groupCount) {
        this.groupCount = groupCount;
        this.groups = new ArrayList<>(groupCount);
        this.groupIds = new UUID[groupCount];
        this.groupData = new CompoundNBT[groupCount];

        for (int i = 0; i < groupCount; i++) {
            groups.add(new HashSet<>());
            groupIds[i] = UUID.randomUUID();
            groupData[i] = new CompoundNBT();
        }
    }

    ///////////////////////////////////////////////////////////////////

    public Collection<TDeviceInfo> getDeviceGroup(final int index) {
        return groups.get(index);
    }

    @Override
    public ListNBT serializeNBT() {
        final ListNBT listTag = new ListNBT();
        for (int i = 0; i < groupCount; i++) {
            serializeDevices(i);

            final CompoundNBT sideTag = new CompoundNBT();

            sideTag.putUUID(GROUP_ID_TAG_NAME, groupIds[i]);
            sideTag.put(GROUP_DATA_TAG_NAME, groupData[i]);

            listTag.add(sideTag);
        }
        return listTag;
    }

    @Override
    public void deserializeNBT(final ListNBT nbt) {
        final int count = Math.min(groupCount, nbt.size());
        for (int i = 0; i < count; i++) {
            final CompoundNBT sideTag = nbt.getCompound(i);

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
            final HashSet<TDeviceInfo> group = groups.get(i);
            for (final TDeviceInfo deviceInfo : group) {
                if (Objects.equals(device, deviceInfo.device)) {
                    return Optional.of(groupIds[i]);
                }
            }
        }
        return super.getDeviceIdentifier(device);
    }

    ///////////////////////////////////////////////////////////////////

    protected final void setDevicesForGroup(final int index, final Set<TDeviceInfo> newDevices) {
        final HashSet<TDeviceInfo> oldDevices = groups.get(index);
        if (Objects.equals(newDevices, oldDevices)) {
            return;
        }

        final HashSet<TDeviceInfo> removedDevices = new HashSet<>(oldDevices);
        removedDevices.removeAll(newDevices);
        for (final TDeviceInfo info : removedDevices) {
            devices.removeInt(info.device);
        }

        final HashSet<TDeviceInfo> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(oldDevices);
        for (final TDeviceInfo info : addedDevices) {
            devices.put(info.device, info.getEnergyConsumption());
        }

        oldDevices.removeAll(removedDevices);
        oldDevices.addAll(newDevices);

        final CompoundNBT devicesTag = groupData[index];
        for (final TDeviceInfo deviceInfo : removedDevices) {
            ItemDeviceUtils.getItemDeviceDataKey(deviceInfo.provider).ifPresent(devicesTag::remove);
        }
        for (final TDeviceInfo deviceInfo : addedDevices) {
            ItemDeviceUtils.getItemDeviceDataKey(deviceInfo.provider).ifPresent(key -> {
                if (devicesTag.contains(key, NBTTagIds.TAG_COMPOUND)) {
                    deviceInfo.device.deserializeNBT(devicesTag.getCompound(key));
                }
            });
        }

        scanDevices();
    }

    ///////////////////////////////////////////////////////////////////

    private void serializeDevices(final int index) {
        final CompoundNBT devicesTag = new CompoundNBT();
        for (final TDeviceInfo deviceInfo : groups.get(index)) {
            ItemDeviceUtils.getItemDeviceDataKey(deviceInfo.provider).ifPresent(key -> {
                final CompoundNBT deviceTag = deviceInfo.device.serializeNBT();
                if (!deviceTag.isEmpty()) {
                    devicesTag.put(key, deviceTag);
                }
            });
        }

        groupData[index] = devicesTag;
    }
}

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
import java.util.stream.Collectors;

public abstract class AbstractGroupingDeviceBusElement<TProvider extends IForgeRegistryEntry<TProvider>, TDeviceInfo extends AbstractDeviceInfo<TProvider, ?>> extends AbstractDeviceBusElement implements INBTSerializable<ListNBT> {
    private static final String GROUP_ID_NBT_TAG_NAME = "groupId";
    private static final String GROUP_DATA_NBT_TAG_NAME = "groupData";

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
        final ListNBT nbt = new ListNBT();
        for (int i = 0; i < groupCount; i++) {
            serializeDevices(i);

            final CompoundNBT sideNbt = new CompoundNBT();

            sideNbt.putUniqueId(GROUP_ID_NBT_TAG_NAME, groupIds[i]);
            sideNbt.put(GROUP_DATA_NBT_TAG_NAME, groupData[i]);

            nbt.add(sideNbt);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(final ListNBT nbt) {
        final int count = Math.min(groupCount, nbt.size());
        for (int i = 0; i < count; i++) {
            final CompoundNBT sideNbt = nbt.getCompound(i);

            if (sideNbt.hasUniqueId(GROUP_ID_NBT_TAG_NAME)) {
                groupIds[i] = sideNbt.getUniqueId(GROUP_ID_NBT_TAG_NAME);
            }
            if (sideNbt.contains(GROUP_DATA_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                groupData[i] = sideNbt.getCompound(GROUP_DATA_NBT_TAG_NAME);
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
        devices.removeAll(removedDevices.stream().map(info -> info.device).collect(Collectors.toList()));

        final HashSet<TDeviceInfo> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(oldDevices);
        devices.addAll(addedDevices.stream().map(info -> info.device).collect(Collectors.toList()));

        oldDevices.removeAll(removedDevices);
        oldDevices.addAll(newDevices);

        final CompoundNBT devicesNbt = groupData[index];
        for (final TDeviceInfo deviceInfo : removedDevices) {
            ItemDeviceUtils.getItemDeviceDataKey(deviceInfo.provider).ifPresent(devicesNbt::remove);
        }
        for (final TDeviceInfo deviceInfo : addedDevices) {
            ItemDeviceUtils.getItemDeviceDataKey(deviceInfo.provider).ifPresent(key -> {
                if (devicesNbt.contains(key, NBTTagIds.TAG_COMPOUND)) {
                    deviceInfo.device.deserializeNBT(devicesNbt.getCompound(key));
                }
            });
        }

        scanDevices();
    }

    ///////////////////////////////////////////////////////////////////

    private void serializeDevices(final int index) {
        final CompoundNBT devicesNbt = new CompoundNBT();
        for (final TDeviceInfo deviceInfo : groups.get(index)) {
            ItemDeviceUtils.getItemDeviceDataKey(deviceInfo.provider).ifPresent(key -> {
                final CompoundNBT deviceNbt = deviceInfo.device.serializeNBT();
                if (!deviceNbt.isEmpty()) {
                    devicesNbt.put(key, deviceNbt);
                }
            });
        }

        groupData[index] = devicesNbt;
    }
}

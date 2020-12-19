package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.bus.device.DeviceInfo;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractGroupingDeviceBusElement extends AbstractDeviceBusElement implements INBTSerializable<ListNBT> {
    private static final String DEVICE_ID_NBT_TAG_NAME = "deviceId";
    private static final String DEVICE_DATA_NBT_TAG_NAME = "deviceData";

    ///////////////////////////////////////////////////////////////////

    protected final int groupCount;
    protected final ArrayList<HashSet<DeviceInfo>> devicesByGroup;

    ///////////////////////////////////////////////////////////////////

    protected final UUID[] deviceIds;
    protected final CompoundNBT[] deviceData;

    ///////////////////////////////////////////////////////////////////

    protected AbstractGroupingDeviceBusElement(final int groupCount) {
        this.groupCount = groupCount;
        this.devicesByGroup = new ArrayList<>(groupCount);
        this.deviceIds = new UUID[groupCount];
        this.deviceData = new CompoundNBT[groupCount];

        for (int i = 0; i < groupCount; i++) {
            devicesByGroup.add(new HashSet<>());
            deviceIds[i] = UUID.randomUUID();
            deviceData[i] = new CompoundNBT();
        }
    }

    @Override
    public ListNBT serializeNBT() {
        serializeDevices();

        final ListNBT nbt = new ListNBT();
        for (int i = 0; i < groupCount; i++) {
            final CompoundNBT sideNbt = new CompoundNBT();

            sideNbt.putUniqueId(DEVICE_ID_NBT_TAG_NAME, deviceIds[i]);
            sideNbt.put(DEVICE_DATA_NBT_TAG_NAME, deviceData[i]);

            nbt.add(sideNbt);
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(final ListNBT nbt) {
        final int count = Math.min(groupCount, nbt.size());
        for (int i = 0; i < count; i++) {
            final CompoundNBT sideNbt = nbt.getCompound(i);

            if (sideNbt.hasUniqueId(DEVICE_ID_NBT_TAG_NAME)) {
                deviceIds[i] = sideNbt.getUniqueId(DEVICE_ID_NBT_TAG_NAME);
            }
            if (sideNbt.contains(DEVICE_DATA_NBT_TAG_NAME, NBTTagIds.TAG_COMPOUND)) {
                deviceData[i] = sideNbt.getCompound(DEVICE_DATA_NBT_TAG_NAME);
            }
        }
    }

    @Override
    public Optional<UUID> getDeviceIdentifier(final Device device) {
        for (int i = 0; i < groupCount; i++) {
            final HashSet<DeviceInfo> group = devicesByGroup.get(i);
            for (final DeviceInfo deviceInfo : group) {
                if (Objects.equals(device, deviceInfo.device)) {
                    return Optional.of(deviceIds[i]);
                }
            }
        }
        return super.getDeviceIdentifier(device);
    }

    ///////////////////////////////////////////////////////////////////

    protected void setDevicesForGroup(final int index, final HashSet<DeviceInfo> newDevices) {
        final HashSet<DeviceInfo> oldDevices = devicesByGroup.get(index);
        if (Objects.equals(newDevices, oldDevices)) {
            return;
        }

        final HashSet<DeviceInfo> removedDevices = new HashSet<>(oldDevices);
        removedDevices.removeAll(newDevices);
        devices.removeAll(removedDevices.stream().map(info -> info.device).collect(Collectors.toList()));

        final HashSet<DeviceInfo> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(oldDevices);
        devices.addAll(addedDevices.stream().map(info -> info.device).collect(Collectors.toList()));

        oldDevices.removeAll(removedDevices);
        oldDevices.addAll(newDevices);

        final CompoundNBT devicesNbt = deviceData[index];
        for (final DeviceInfo deviceInfo : removedDevices) {
            getSerializationKey(deviceInfo).ifPresent(devicesNbt::remove);
        }
        for (final DeviceInfo deviceInfo : addedDevices) {
            getSerializationKey(deviceInfo).ifPresent(key -> {
                if (devicesNbt.contains(key, NBTTagIds.TAG_COMPOUND)) {
                    deviceInfo.device.deserializeNBT(devicesNbt.getCompound(key));
                }
            });
        }

        scanDevices();
    }

    ///////////////////////////////////////////////////////////////////

    private void serializeDevices() {
        for (int i = 0; i < groupCount; i++) {
            final CompoundNBT devicesNbt = new CompoundNBT();
            for (final DeviceInfo deviceInfo : devicesByGroup.get(i)) {
                getSerializationKey(deviceInfo).ifPresent(key -> {
                    final CompoundNBT deviceNbt = deviceInfo.device.serializeNBT();
                    if (!deviceNbt.isEmpty()) {
                        devicesNbt.put(key, deviceNbt);
                    }
                });
            }
            deviceData[i] = devicesNbt;
        }
    }

    private static Optional<String> getSerializationKey(final DeviceInfo info) {
        final ResourceLocation providerName = info.provider.getRegistryName();
        if (providerName == null) {
            return Optional.empty();
        }

        return Optional.of(providerName.toString());
    }
}

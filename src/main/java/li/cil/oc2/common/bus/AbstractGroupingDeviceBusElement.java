package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.common.util.NBTTagIds;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;

public abstract class AbstractGroupingDeviceBusElement extends AbstractDeviceBusElement implements INBTSerializable<ListNBT> {
    private static final String DEVICE_ID_NBT_TAG_NAME = "deviceId";
    private static final String DEVICE_DATA_NBT_TAG_NAME = "deviceData";

    ///////////////////////////////////////////////////////////////////

    protected final int groupCount;
    protected final ArrayList<HashSet<Device>> devicesByGroup;

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
        final ListNBT nbt = new ListNBT();
        for (int i = 0; i < groupCount; i++) {
            final CompoundNBT sideNbt = new CompoundNBT();

            sideNbt.putUniqueId(DEVICE_ID_NBT_TAG_NAME, deviceIds[i]);

            final CompoundNBT devicesNbt = deviceData[i];
            for (final Device device : devicesByGroup.get(i)) {
                device.getSerializationKey().ifPresent(key ->
                        devicesNbt.put(key.toString(), device.serializeNBT()));
            }
            sideNbt.put(DEVICE_DATA_NBT_TAG_NAME, devicesNbt);

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
            if (devicesByGroup.get(i).contains(device)) {
                return Optional.of(deviceIds[i]);
            }
        }
        return super.getDeviceIdentifier(device);
    }

    ///////////////////////////////////////////////////////////////////

    protected void setDevicesForGroup(final int index, final HashSet<Device> newDevices) {
        final HashSet<Device> oldDevices = devicesByGroup.get(index);
        if (Objects.equals(newDevices, oldDevices)) {
            return;
        }

        final HashSet<Device> removedDevices = new HashSet<>(oldDevices);
        removedDevices.removeAll(newDevices);
        devices.removeAll(removedDevices);

        final HashSet<Device> addedDevices = new HashSet<>(newDevices);
        addedDevices.removeAll(oldDevices);
        devices.addAll(addedDevices);

        final CompoundNBT devicesNbt = deviceData[index];
        for (final Device device : removedDevices) {
            device.getSerializationKey().ifPresent(key ->
                    devicesNbt.remove(key.toString()));
        }
        for (final Device device : addedDevices) {
            device.getSerializationKey().ifPresent(key -> {
                if (devicesNbt.contains(key.toString(), NBTTagIds.TAG_COMPOUND)) {
                    device.deserializeNBT(devicesNbt.getCompound(key.toString()));
                }
            });
        }

        scanDevices();
    }
}

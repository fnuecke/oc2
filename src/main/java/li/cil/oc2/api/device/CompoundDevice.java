package li.cil.oc2.api.device;

import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A utility device type that allows grouping multiple {@link Device} instances.
 * <p>
 * Serialization of contained devices requires the added devices' unique id to
 * have been restored prior to calling {@link #deserializeNBT(CompoundNBT)}.
 */
public class CompoundDevice extends AbstractDevice {
    private final ArrayList<Device> devices;

    public CompoundDevice(final Collection<Device> devices) {
        this.devices = new ArrayList<>(devices);
    }

    public CompoundDevice(final Device... devices) {
        this(Arrays.asList(devices));
    }

    public CompoundDevice() {
        this(Collections.emptyList());
    }

    /**
     * The list of devices grouped in this device.
     * <p>
     * Use this in case you need to inspect the current list of devices, add new
     * devices or remove existing devices.
     */
    public List<Device> getDevices() {
        return devices;
    }

    @Override
    public List<String> getTypeNames() {
        return devices.stream()
                .map(Device::getTypeNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return devices.stream()
                .map(Device::getMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("rawtypes")
    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT nbt = super.serializeNBT();

        final CompoundNBT devicesNbt = new CompoundNBT();
        for (final Device device : devices) {
            if (device instanceof INBTSerializable) {
                final INBTSerializable serializable = (INBTSerializable) device;
                final String uuid = device.getUniqueId().toString();
                final INBT deviceNbt = serializable.serializeNBT();
                devicesNbt.put(uuid, deviceNbt);
            }
        }
        nbt.put("devices", devicesNbt);

        return nbt;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public void deserializeNBT(final CompoundNBT nbt) {
        super.deserializeNBT(nbt);

        final CompoundNBT devicesNbt = nbt.getCompound("devices");
        for (final Device device : devices) {
            final String uuid = device.getUniqueId().toString();
            if (!devicesNbt.contains(uuid)) {
                continue;
            }

            if (device instanceof INBTSerializable) {
                final INBTSerializable serializable = (INBTSerializable) device;
                final INBT deviceNbt = devicesNbt.get(uuid);
                serializable.deserializeNBT(deviceNbt);
            }
        }
    }
}

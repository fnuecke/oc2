package li.cil.oc2.common.device;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.DeviceMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class CompoundDevice implements Device {
    private final ArrayList<Device> devices;

    public CompoundDevice(final ArrayList<Device> devices) {
        this.devices = devices;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final CompoundDevice that = (CompoundDevice) o;
        return devices.equals(that.devices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(devices);
    }
}

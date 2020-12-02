package li.cil.oc2.common.device;

import li.cil.oc2.api.bus.device.DeviceInterface;
import li.cil.oc2.api.bus.device.DeviceMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class DeviceInterfaceCollection implements DeviceInterface {
    private final ArrayList<DeviceInterface> deviceInterfaces;

    public DeviceInterfaceCollection(final ArrayList<DeviceInterface> deviceInterfaces) {
        this.deviceInterfaces = deviceInterfaces;
    }

    @Override
    public List<String> getTypeNames() {
        return deviceInterfaces.stream()
                .map(DeviceInterface::getTypeNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<DeviceMethod> getMethods() {
        return deviceInterfaces.stream()
                .map(DeviceInterface::getMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final DeviceInterfaceCollection that = (DeviceInterfaceCollection) o;
        return deviceInterfaces.equals(that.deviceInterfaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceInterfaces);
    }
}

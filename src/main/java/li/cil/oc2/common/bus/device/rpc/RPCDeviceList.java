package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class RPCDeviceList implements RPCDevice {
    private final ArrayList<RPCDevice> deviceInterfaces;

    public RPCDeviceList(final ArrayList<RPCDevice> deviceInterfaces) {
        this.deviceInterfaces = deviceInterfaces;
    }

    @Override
    public List<String> getTypeNames() {
        return deviceInterfaces.stream()
                .map(RPCDevice::getTypeNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<RPCMethod> getMethods() {
        return deviceInterfaces.stream()
                .map(RPCDevice::getMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RPCDeviceList that = (RPCDeviceList) o;
        return deviceInterfaces.equals(that.deviceInterfaces);
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceInterfaces);
    }
}

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class RPCDeviceList implements RPCDevice {
    private final ArrayList<RPCDevice> devices;

    ///////////////////////////////////////////////////////////////////

    public RPCDeviceList(final ArrayList<RPCDevice> devices) {
        this.devices = devices;
    }

    @Override
    public List<String> getTypeNames() {
        return devices.stream()
                .map(RPCDevice::getTypeNames)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<RPCMethod> getMethods() {
        return devices.stream()
                .map(RPCDevice::getMethods)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final RPCDeviceList that = (RPCDeviceList) o;
        return devices.equals(that.devices);
    }

    @Override
    public int hashCode() {
        return Objects.hash(devices);
    }
}

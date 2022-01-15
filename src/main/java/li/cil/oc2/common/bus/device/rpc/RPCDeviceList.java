package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public record RPCDeviceList(ArrayList<RPCDevice> devices) implements RPCDevice {
    @Override
    public List<String> getTypeNames() {
        return devices.stream()
            .map(RPCDevice::getTypeNames)
            .flatMap(Collection::stream)
            .distinct()
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
    public void mount() {
        for (final RPCDevice device : devices) {
            device.mount();
        }
    }

    @Override
    public void unmount() {
        for (final RPCDevice device : devices) {
            device.unmount();
        }
    }

    @Override
    public void suspend() {
        for (final RPCDevice device : devices) {
            device.suspend();
        }
    }
}

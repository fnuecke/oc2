package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public record TypeNameRPCDevice(String typeName) implements RPCDevice, ItemDevice {
    @Override
    public List<String> getTypeNames() {
        return singletonList(typeName);
    }

    @Override
    public List<RPCMethod> getMethods() {
        return emptyList();
    }
}

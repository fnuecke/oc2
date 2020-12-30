package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

import java.util.Collections;
import java.util.List;

public final class TypeNameRPCDevice implements RPCDevice, ItemDevice {
    private final String typeName;

    public TypeNameRPCDevice(final String typeName) {
        this.typeName = typeName;
    }

    @Override
    public List<String> getTypeNames() {
        return Collections.singletonList(typeName);
    }

    @Override
    public List<RPCMethod> getMethods() {
        return Collections.emptyList();
    }
}

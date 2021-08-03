package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethod;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public final class TypeNameRPCDevice implements RPCDevice, ItemDevice {
    private final String typeName;

    public TypeNameRPCDevice(final String typeName) {
        this.typeName = typeName;
    }

    @Override
    public List<String> getTypeNames() {
        return singletonList(typeName);
    }

    @Override
    public List<RPCMethod> getMethods() {
        return emptyList();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        final TypeNameRPCDevice that = (TypeNameRPCDevice) o;
        return typeName.equals(that.typeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(typeName);
    }
}

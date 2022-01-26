package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;
import net.minecraft.nbt.CompoundTag;

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
    public List<RPCMethodGroup> getMethodGroups() {
        return devices.stream()
            .map(RPCDevice::getMethodGroups)
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

    // NB: We only use the list device in the adapter, for referencing grouped devices by their ID.
    //     As such, serialize/deserialize will never be called on this class.

    @Override
    public CompoundTag serializeNBT() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deserializeNBT(final CompoundTag tag) {
        throw new UnsupportedOperationException();
    }
}

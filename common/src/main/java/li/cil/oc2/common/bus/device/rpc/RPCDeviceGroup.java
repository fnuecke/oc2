/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.rpc.RPCBusContext;
import li.cil.oc2.api.bus.device.rpc.RPCDevice;
import li.cil.oc2.api.bus.device.rpc.RPCMethodGroup;

import java.util.Collection;
import java.util.List;

public record RPCDeviceGroup(List<RPCDevice> devices, List<String> typeNames) {
    public List<RPCMethodGroup> getMethodGroups() {
        return devices.stream()
            .map(RPCDevice::getMethodGroups)
            .flatMap(Collection::stream)
            .toList();
    }

    public void mount(final RPCBusContext context) {
        for (final RPCDevice device : devices) {
            device.mount(context);
        }
    }

    public void unmount(final RPCBusContext context) {
        for (final RPCDevice device : devices) {
            device.unmount(context);
        }
    }
}

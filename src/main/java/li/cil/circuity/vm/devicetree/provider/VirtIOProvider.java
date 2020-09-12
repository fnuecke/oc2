package li.cil.circuity.vm.devicetree.provider;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.devicetree.DeviceTree;
import li.cil.circuity.api.vm.devicetree.DeviceTreePropertyNames;
import li.cil.circuity.api.vm.devicetree.DeviceTreeProvider;

public final class VirtIOProvider implements DeviceTreeProvider {
    public static final DeviceTreeProvider INSTANCE = new VirtIOProvider();

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        node.addProp(DeviceTreePropertyNames.COMPATIBLE, "virtio,mmio");
    }
}

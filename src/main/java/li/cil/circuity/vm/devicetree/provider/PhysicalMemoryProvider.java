package li.cil.circuity.vm.devicetree.provider;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.devicetree.DeviceNames;
import li.cil.circuity.api.vm.devicetree.DeviceTree;
import li.cil.circuity.api.vm.devicetree.DeviceTreePropertyNames;
import li.cil.circuity.api.vm.devicetree.DeviceTreeProvider;

import java.util.Optional;

public class PhysicalMemoryProvider implements DeviceTreeProvider {
    public static final DeviceTreeProvider INSTANCE = new PhysicalMemoryProvider();

    @Override
    public Optional<String> getName(final Device device) {
        return Optional.of(DeviceNames.MEMORY);
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        node.addProp(DeviceTreePropertyNames.DEVICE_TYPE, DeviceNames.MEMORY);
    }
}

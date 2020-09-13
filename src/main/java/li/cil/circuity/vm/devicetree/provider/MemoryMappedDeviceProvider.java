package li.cil.circuity.vm.devicetree.provider;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.MemoryRange;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.devicetree.DeviceTree;
import li.cil.circuity.api.vm.devicetree.DeviceTreePropertyNames;
import li.cil.circuity.api.vm.devicetree.DeviceTreeProvider;

import java.util.Optional;

public class MemoryMappedDeviceProvider implements DeviceTreeProvider {
    public static final DeviceTreeProvider INSTANCE = new MemoryMappedDeviceProvider();

    @Override
    public Optional<DeviceTree> createNode(final DeviceTree root, final MemoryMap memoryMap, final Device device, final String deviceName) {
        final Optional<MemoryRange> range = memoryMap.getMemoryRange((MemoryMappedDevice) device);
        return range.map(r -> root.getChild(deviceName, r.address()));
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        final MemoryMappedDevice mappedDevice = (MemoryMappedDevice) device;
        final Optional<MemoryRange> range = memoryMap.getMemoryRange(mappedDevice);

        // TODO in the future when we may want to change bus widths check parent for cell and size cell num.
        range.ifPresent(r -> node.addProp(DeviceTreePropertyNames.REG,
                ((long) r.address()) & 0xFFFFFFFFL,
                ((long) r.size()) & 0xFFFFFFFFL));
    }
}

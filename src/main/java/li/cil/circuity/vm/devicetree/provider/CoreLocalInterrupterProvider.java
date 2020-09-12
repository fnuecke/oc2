package li.cil.circuity.vm.devicetree.provider;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.devicetree.DeviceTree;
import li.cil.circuity.api.vm.devicetree.DeviceTreePropertyNames;
import li.cil.circuity.api.vm.devicetree.DeviceTreeProvider;

import java.util.Optional;

public final class CoreLocalInterrupterProvider implements DeviceTreeProvider {
    public static final DeviceTreeProvider INSTANCE = new CoreLocalInterrupterProvider();

    @Override
    public Optional<String> getName(final Device device) {
        return Optional.of("clint");
    }

    @Override
    public Optional<DeviceTree> createNode(final DeviceTree root, final MemoryMap memoryMap, final Device device, final String deviceName) {
        return Optional.of(root.find("/soc").getChild(deviceName, memoryMap.getDeviceAddress((MemoryMappedDevice) device)));
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        node.addProp(DeviceTreePropertyNames.COMPATIBLE, "riscv,clint0");
    }
}

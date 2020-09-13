package li.cil.circuity.vm.devicetree.provider;

import li.cil.circuity.api.vm.MemoryMap;
import li.cil.circuity.api.vm.MemoryRange;
import li.cil.circuity.api.vm.device.Device;
import li.cil.circuity.api.vm.device.memory.MemoryMappedDevice;
import li.cil.circuity.api.vm.devicetree.DeviceNames;
import li.cil.circuity.api.vm.devicetree.DeviceTree;
import li.cil.circuity.api.vm.devicetree.DeviceTreePropertyNames;
import li.cil.circuity.api.vm.devicetree.DeviceTreeProvider;
import li.cil.circuity.vm.riscv.device.R5PlatformLevelInterruptController;

import java.util.Optional;

public final class PlatformLevelInterruptControllerProvider implements DeviceTreeProvider {
    public static final DeviceTreeProvider INSTANCE = new PlatformLevelInterruptControllerProvider();

    @Override
    public Optional<String> getName(final Device device) {
        return Optional.of("plic");
    }

    @Override
    public Optional<DeviceTree> createNode(final DeviceTree root, final MemoryMap memoryMap, final Device device, final String deviceName) {
        final Optional<MemoryRange> range = memoryMap.getMemoryRange((MemoryMappedDevice) device);
        return range.map(r -> root.find("/soc").getChild(deviceName, r.address()));
    }

    @Override
    public void visit(final DeviceTree node, final MemoryMap memoryMap, final Device device) {
        final R5PlatformLevelInterruptController plic = (R5PlatformLevelInterruptController) device;
        node
                .addProp("#address-cells", 0)
                .addProp("#interrupt-cells", 1)
                .addProp(DeviceNames.INTERRUPT_CONTROLLER)
                .addProp(DeviceTreePropertyNames.COMPATIBLE, "riscv,plic0")
                .addProp("riscv,ndev", 31)
                .addProp(DeviceTreePropertyNames.PHANDLE, node.createPHandle(plic));
    }
}

package li.cil.oc2.common.bus.device.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.sedna.api.memory.MemoryMap;
import net.minecraft.item.ItemStack;

public final class FirmwareFlashMemoryVMDevice extends IdentityProxy<ItemStack> implements VMDevice, VMDeviceLifecycleListener, ItemDevice {
    private final Firmware firmware;
    private MemoryMap memoryMap;

    ///////////////////////////////////////////////////////////////

    public FirmwareFlashMemoryVMDevice(final ItemStack identity, final Firmware firmware) {
        super(identity);
        this.firmware = firmware;
    }

    ///////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult load(final VMContext context) {
        memoryMap = context.getMemoryMap();

        return VMDeviceLoadResult.success();
    }

    @Override
    public void handleLifecycleEvent(final VMDeviceLifecycleEventType event) {
        switch (event) {
            case INITIALIZING:
                // TODO Have start address passed with event?
                firmware.run(memoryMap, 0x80000000L);
                break;
            case UNLOAD:
                memoryMap = null;
                break;
        }
    }
}

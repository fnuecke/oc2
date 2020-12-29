package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.bus.device.vm.*;
import li.cil.sedna.api.memory.MemoryMap;
import net.minecraft.item.ItemStack;

public final class FirmwareFlashMemoryDevice extends AbstractItemDevice implements VMDevice, VMDeviceLifecycleListener {
    private final Firmware firmware;
    private MemoryMap memoryMap;

    ///////////////////////////////////////////////////////////////

    public FirmwareFlashMemoryDevice(final ItemStack stack, final Firmware firmware) {
        super(stack);
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
            case INITIALIZE:
                // TODO Have start address passed with event?
                firmware.run(memoryMap, 0x80000000L);
                break;
            case UNLOAD:
                memoryMap = null;
                break;
        }
    }
}

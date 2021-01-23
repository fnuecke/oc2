package li.cil.oc2.common.bus.device.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.bus.device.data.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMContext;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.sedna.api.memory.MemoryMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.TranslationTextComponent;

@SuppressWarnings("UnstableApiUsage")
public final class FirmwareFlashMemoryVMDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, FirmwareLoader {
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

        context.getEventBus().register(this);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unload() {
        memoryMap = null;
    }

    @Subscribe
    public void handleInitializingEvent(final VMInitializingEvent event) {
        copyDataToMemory(event.getProgramStartAddress());
    }

    ///////////////////////////////////////////////////////////////

    private void copyDataToMemory(final long address) {
        if (!firmware.run(memoryMap, address)) {
            throw new VMInitializationException(new TranslationTextComponent(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
        }
    }
}

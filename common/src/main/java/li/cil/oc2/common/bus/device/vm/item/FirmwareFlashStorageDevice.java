/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.item;

import com.google.common.eventbus.Subscribe;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.bus.device.vm.FirmwareLoader;
import li.cil.oc2.api.bus.device.vm.VMDevice;
import li.cil.oc2.api.bus.device.vm.VMDeviceLoadResult;
import li.cil.oc2.api.bus.device.vm.context.VMContext;
import li.cil.oc2.api.bus.device.vm.event.VMInitializationException;
import li.cil.oc2.api.bus.device.vm.event.VMInitializingEvent;
import li.cil.oc2.common.Constants;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.sedna.api.memory.MemoryMap;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class FirmwareFlashStorageDevice extends IdentityProxy<ItemStack> implements VMDevice, ItemDevice, FirmwareLoader {
    private final Firmware firmware;
    private MemoryMap memoryMap;

    ///////////////////////////////////////////////////////////////

    public FirmwareFlashStorageDevice(final ItemStack identity, final Firmware firmware) {
        super(identity);
        this.firmware = firmware;
    }

    ///////////////////////////////////////////////////////////////

    @Override
    public VMDeviceLoadResult mount(final VMContext context) {
        memoryMap = context.getMemoryMap();

        context.getEventBus().register(this);

        return VMDeviceLoadResult.success();
    }

    @Override
    public void unmount() {
        memoryMap = null;
    }

    @Override
    public void dispose() {
    }

    @Subscribe
    public void handleInitializingEvent(final VMInitializingEvent event) {
        copyDataToMemory(event.programStartAddress());
    }

    ///////////////////////////////////////////////////////////////

    private void copyDataToMemory(final long address) {
        if (!firmware.run(memoryMap, address)) {
            throw new VMInitializationException(Component.translatable(Constants.COMPUTER_ERROR_INSUFFICIENT_MEMORY));
        }
    }
}

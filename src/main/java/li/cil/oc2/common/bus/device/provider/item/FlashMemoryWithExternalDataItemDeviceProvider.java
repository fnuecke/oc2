/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.data.Firmware;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.item.FirmwareFlashMemoryVMDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.FlashMemoryWithExternalDataItem;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class FlashMemoryWithExternalDataItemDeviceProvider extends AbstractItemDeviceProvider {
    public FlashMemoryWithExternalDataItemDeviceProvider() {
        super(FlashMemoryWithExternalDataItem.class);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final ItemStack stack = query.getItemStack();
        final FlashMemoryWithExternalDataItem item = (FlashMemoryWithExternalDataItem) stack.getItem();

        final Firmware firmware = item.getFirmware(stack);
        if (firmware == null) {
            return Optional.empty();
        }

        return Optional.of(new FirmwareFlashMemoryVMDevice(stack, firmware));
    }
}

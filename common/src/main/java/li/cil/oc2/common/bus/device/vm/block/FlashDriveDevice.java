/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.vm.block;

import li.cil.oc2.api.bus.device.data.BlockDeviceData;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.provider.item.FlashMemoryItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.FloppyMedia;
import li.cil.oc2.common.item.FlashMemoryItem;
import net.minecraft.world.item.ItemStack;

public final class FlashDriveDevice extends AbstractRemovableMediaDevice {
    public FlashDriveDevice(final RemovableMediaContainer container) {
        super(container);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected int getMediumCapacity(final ItemStack stack) {
        if (!(stack.getItem() instanceof final FlashMemoryItem flash)) {
            return 0;
        }

        return Math.min(flash.getCapacity(stack), Config.maxBlobCapacity);
    }

    @Override
    protected boolean isSupportedMedium(final ItemStack stack) {
        return stack.getItem() instanceof FlashMemoryItem;
    }

    @Override
    public String getDeviceDataKey() {
        return FlashMemoryItemDeviceProvider.DEVICE_DATA_KEY;
    }

    @Override
    protected MediumInitializer createMediumInitializer(final ItemStack stack) {
        final BlockDeviceData data = ((FlashMemoryItem) stack.getItem()).getData(stack);
        if (data == null) {
            return LEAVE_BLANK;
        }

        return medium -> FloppyMedia.image(medium, data.getBlockDevice());
    }
}

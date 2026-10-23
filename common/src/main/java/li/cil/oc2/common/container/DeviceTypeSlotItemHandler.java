/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.common.inventory.ItemHandler;
import net.minecraft.resources.Identifier;

import javax.annotation.Nullable;

public final class DeviceTypeSlotItemHandler extends SlotItemHandler {
    private final DeviceType deviceType;

    public DeviceTypeSlotItemHandler(final ItemHandler itemHandler, final DeviceType deviceType, final int index, final int xPosition, final int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.deviceType = deviceType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    @Nullable
    @Override
    public Identifier getNoItemIcon() {
        if (hasItem()) {
            return super.getNoItemIcon();
        } else {
            return deviceType.getBackgroundIcon();
        }
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.inventory.ItemHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;

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
    public Pair<ResourceLocation, ResourceLocation> getNoItemIcon() {
        if (hasItem()) {
            return super.getNoItemIcon();
        } else {
            return Pair.of(InventoryMenu.BLOCK_ATLAS, deviceType.getBackgroundIcon());
        }
    }
}

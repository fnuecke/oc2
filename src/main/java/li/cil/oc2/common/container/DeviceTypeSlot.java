package li.cil.oc2.common.container;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.bus.device.DeviceType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;

public final class DeviceTypeSlot extends Slot {
    private final DeviceType deviceType;

    public DeviceTypeSlot(final Container container, final DeviceType deviceType, final int index, final int x, final int y) {
        super(container, index, x, y);
        this.deviceType = deviceType;
    }

    public DeviceType getDeviceType() {
        return deviceType;
    }

    @Override
    public boolean mayPlace(@NotNull final ItemStack stack) {
        return super.mayPlace(stack) && stack.is(deviceType.getTag());
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

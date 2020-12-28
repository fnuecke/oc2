package li.cil.oc2.common.container;

import com.mojang.datafixers.util.Pair;
import li.cil.oc2.api.bus.device.DeviceType;
import net.minecraft.inventory.container.PlayerContainer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nullable;

public final class TypedSlotItemHandler extends SlotItemHandler {
    private final DeviceType deviceType;

    public TypedSlotItemHandler(final IItemHandler itemHandler, final DeviceType deviceType, final int index, final int xPosition, final int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
        this.deviceType = deviceType;
    }

    @Nullable
    @Override
    public Pair<ResourceLocation, ResourceLocation> getBackground() {
        if (getHasStack()) {
            return super.getBackground();
        } else {
            return Pair.of(PlayerContainer.LOCATION_BLOCKS_TEXTURE, deviceType.getIcon());
        }
    }
}

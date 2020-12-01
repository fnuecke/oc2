package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDevice;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerDeviceProvider extends AbstractCapabilityAnyTileEntityDeviceProvider<IItemHandler> {
    public ItemHandlerDeviceProvider() {
        super(() -> Capabilities.ITEM_HANDLER_CAPABILITY);
    }

    @Override
    protected LazyOptional<Device> getDevice(final BlockDeviceQuery query, final IItemHandler value) {
        return LazyOptional.of(() -> new ItemHandlerDevice(value));
    }

    public static final class ItemHandlerDevice extends ObjectDevice {
        private final IItemHandler itemHandler;

        public ItemHandlerDevice(final IItemHandler itemHandler) {
            super("itemHandler");
            this.itemHandler = itemHandler;
        }

        @Callback
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Callback
        public ItemStack getStackInSlot(final int slot) {
            return itemHandler.getStackInSlot(slot);
        }

        @Callback
        public int getSlotLimit(final int slot) {
            return itemHandler.getSlotLimit(slot);
        }
    }
}

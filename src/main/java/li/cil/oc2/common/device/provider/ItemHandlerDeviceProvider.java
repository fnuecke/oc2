package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerDeviceProvider extends AbstractCapabilityAnyTileEntityDeviceProvider<IItemHandler> {
    private static final String ITEM_HANDLER_TYPE_NAME = "itemHandler";

    public ItemHandlerDeviceProvider() {
        super(() -> Capabilities.ITEM_HANDLER_CAPABILITY);
    }

    @Override
    protected LazyOptional<Device> getDeviceInterface(final BlockDeviceQuery query, final IItemHandler value) {
        return LazyOptional.of(() -> new ObjectDevice(new ItemHandlerDevice(value), ITEM_HANDLER_TYPE_NAME));
    }

    public static final class ItemHandlerDevice extends AbstractObjectProxy<IItemHandler> {
        public ItemHandlerDevice(final IItemHandler itemHandler) {
            super(itemHandler);
        }

        @Callback
        public int getSlots() {
            return value.getSlots();
        }

        @Callback
        public ItemStack getStackInSlot(final int slot) {
            return value.getStackInSlot(slot);
        }

        @Callback
        public int getSlotLimit(final int slot) {
            return value.getSlotLimit(slot);
        }
    }
}

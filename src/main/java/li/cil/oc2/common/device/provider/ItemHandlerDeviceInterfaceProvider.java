package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.object.Callback;
import li.cil.oc2.api.device.object.ObjectDeviceInterface;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public class ItemHandlerDeviceInterfaceProvider extends AbstractCapabilityAnyTileEntityDeviceInterfaceProvider<IItemHandler> {
    private static final String ITEM_HANDLER_TYPE_NAME = "itemHandler";

    public ItemHandlerDeviceInterfaceProvider() {
        super(() -> Capabilities.ITEM_HANDLER_CAPABILITY);
    }

    @Override
    protected LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery query, final IItemHandler value) {
        return LazyOptional.of(() -> new ObjectDeviceInterface(new ItemHandlerDevice(value), ITEM_HANDLER_TYPE_NAME));
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

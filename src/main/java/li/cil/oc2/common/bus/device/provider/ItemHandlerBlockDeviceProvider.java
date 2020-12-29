package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.AbstractObjectDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityCapabilityDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

public final class ItemHandlerBlockDeviceProvider extends AbstractTileEntityCapabilityDeviceProvider<IItemHandler, TileEntity> {
    private static final String ITEM_HANDLER_TYPE_NAME = "itemHandler";

    ///////////////////////////////////////////////////////////////////

    public ItemHandlerBlockDeviceProvider() {
        super(() -> Capabilities.ITEM_HANDLER);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final IItemHandler value) {
        return LazyOptional.of(() -> new ObjectDevice(new ItemHandlerDevice(value), ITEM_HANDLER_TYPE_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class ItemHandlerDevice extends AbstractObjectDevice<IItemHandler> {
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

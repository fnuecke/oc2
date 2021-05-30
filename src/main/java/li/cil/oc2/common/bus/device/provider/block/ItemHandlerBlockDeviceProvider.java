package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityCapabilityDeviceProvider;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.BlockEntity;
import net.minecraftforge.common.util.Optional;
import net.minecraftforge.items.IItemHandler;

public final class ItemHandlerBlockDeviceProvider extends AbstractBlockEntityCapabilityDeviceProvider<IItemHandler, BlockEntity> {
    public ItemHandlerBlockDeviceProvider() {
        super(() -> Capabilities.ITEM_HANDLER);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final IItemHandler value) {
        return Optional.of(() -> new ObjectDevice(new ItemHandlerDevice(value), "item_handler"));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class ItemHandlerDevice extends IdentityProxy<IItemHandler> {
        public ItemHandlerDevice(final IItemHandler identity) {
            super(identity);
        }

        @Callback
        public int getSlots() {
            return identity.getSlots();
        }

        @Callback
        public ItemStack getStackInSlot(final int slot) {
            return identity.getStackInSlot(slot);
        }

        @Callback
        public int getSlotLimit(final int slot) {
            return identity.getSlotLimit(slot);
        }
    }
}

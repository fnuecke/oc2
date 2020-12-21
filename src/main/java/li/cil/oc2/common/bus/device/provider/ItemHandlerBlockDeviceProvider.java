package li.cil.oc2.common.bus.device.provider;

import alexiil.mc.lib.attributes.item.FixedItemInvView;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.AbstractObjectDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockAttributeDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;

import java.util.Optional;

public final class ItemHandlerBlockDeviceProvider extends AbstractBlockAttributeDeviceProvider<FixedItemInvView, BlockEntity> {
    private static final String ITEM_HANDLER_TYPE_NAME = "itemHandler";

    ///////////////////////////////////////////////////////////////////

    public ItemHandlerBlockDeviceProvider() {
        super(() -> Capabilities.ITEM_HANDLER_CAPABILITY);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final FixedItemInvView value) {
        return Optional.of(new ObjectDevice(new ItemHandlerDevice(value), ITEM_HANDLER_TYPE_NAME));
    }

    ///////////////////////////////////////////////////////////////////

    public static final class ItemHandlerDevice extends AbstractObjectDevice<FixedItemInvView> {
        public ItemHandlerDevice(final FixedItemInvView itemHandler) {
            super(itemHandler);
        }

        @Callback
        public int getSlots() {
            return value.getSlotCount();
        }

        @Callback
        public ItemStack getStackInSlot(final int slot) {
            return value.getInvStack(slot);
        }
    }
}

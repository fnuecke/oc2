package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.item.RedstoneInterfaceCardItemDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class RedstoneInterfaceCardItemDeviceProvider extends AbstractItemDeviceProvider {
    public RedstoneInterfaceCardItemDeviceProvider() {
        super(Items.REDSTONE_INTERFACE_CARD_ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerTileEntity().map(tileEntity ->
                new RedstoneInterfaceCardItemDevice(query.getItemStack(), tileEntity));
    }
}

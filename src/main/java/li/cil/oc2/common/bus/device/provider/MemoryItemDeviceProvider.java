package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.MemoryDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.init.Items;

import java.util.Optional;

public final class MemoryItemDeviceProvider extends AbstractItemDeviceProvider {
    public MemoryItemDeviceProvider() {
        super(Items.RAM_ITEM);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return Optional.of(new MemoryDevice(query.getItemStack()));
    }
}

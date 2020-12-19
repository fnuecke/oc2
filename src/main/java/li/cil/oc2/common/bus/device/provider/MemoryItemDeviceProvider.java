package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.MemoryDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.init.Items;
import net.minecraftforge.common.util.LazyOptional;

public final class MemoryItemDeviceProvider extends AbstractItemDeviceProvider {
    public MemoryItemDeviceProvider() {
        super(Items.RAM_8M_ITEM.get());
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected LazyOptional<Device> getItemDevice(final ItemDeviceQuery query) {
        return LazyOptional.of(() -> new MemoryDevice(query.getItemStack()));
    }
}

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.init.Providers;

public class ItemDeviceInfo extends AbstractDeviceInfo<ItemDeviceProvider, ItemDevice> {
    public ItemDeviceInfo(final ItemDeviceProvider itemDeviceProvider, final ItemDevice device) {
        super(Providers.ITEM_DEVICE_PROVIDER_REGISTRY, itemDeviceProvider, device);
    }
}

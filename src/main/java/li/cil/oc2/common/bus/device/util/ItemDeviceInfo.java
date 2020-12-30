package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;

import javax.annotation.Nullable;

public class ItemDeviceInfo extends AbstractDeviceInfo<ItemDeviceProvider, ItemDevice> {
    public ItemDeviceInfo(@Nullable final ItemDeviceProvider itemDeviceProvider, final ItemDevice device) {
        super(itemDeviceProvider, device);
    }
}

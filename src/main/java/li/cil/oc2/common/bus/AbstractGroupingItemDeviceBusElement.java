package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.common.bus.device.ItemDeviceInfo;

public abstract class AbstractGroupingItemDeviceBusElement extends AbstractGroupingDeviceBusElement<ItemDeviceProvider, ItemDeviceInfo> {
    public AbstractGroupingItemDeviceBusElement(final int groupCount) {
        super(groupCount);
    }
}

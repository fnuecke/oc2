package li.cil.oc2.common.bus;

import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.common.bus.device.util.BlockDeviceInfo;

public abstract class AbstractGroupingBlockDeviceBusElement extends AbstractGroupingDeviceBusElement<BlockDeviceProvider, BlockDeviceInfo> {
    public AbstractGroupingBlockDeviceBusElement(final int groupCount) {
        super(groupCount);
    }
}

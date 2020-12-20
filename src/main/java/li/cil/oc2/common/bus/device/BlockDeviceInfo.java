package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;

public final class BlockDeviceInfo extends AbstractDeviceInfo<BlockDeviceProvider, Device> {
    public BlockDeviceInfo(final BlockDeviceProvider blockDeviceProvider, final Device device) {
        super(blockDeviceProvider, device);
    }
}

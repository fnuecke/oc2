package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;

import javax.annotation.Nullable;

public final class BlockDeviceInfo extends AbstractDeviceInfo<BlockDeviceProvider, Device> {
    public BlockDeviceInfo(@Nullable final BlockDeviceProvider blockDeviceProvider, final Device device) {
        super(blockDeviceProvider, device);
    }
}

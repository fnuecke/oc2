package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.common.init.Providers;

public final class BlockDeviceInfo extends AbstractDeviceInfo<BlockDeviceProvider, Device> {
    public BlockDeviceInfo(final BlockDeviceProvider blockDeviceProvider, final Device device) {
        super(Providers.BLOCK_DEVICE_PROVIDER_REGISTRY, blockDeviceProvider, device);
    }
}

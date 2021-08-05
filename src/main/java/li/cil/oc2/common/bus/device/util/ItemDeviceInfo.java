package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;

import javax.annotation.Nullable;

public final class ItemDeviceInfo extends AbstractDeviceInfo<ItemDeviceProvider, ItemDevice> {
    public final int energyConsumption;

    ///////////////////////////////////////////////////////////////////

    public ItemDeviceInfo(@Nullable final ItemDeviceProvider itemDeviceProvider, final ItemDevice device, final int energyConsumption) {
        super(itemDeviceProvider, device);
        this.energyConsumption = energyConsumption;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    public int getEnergyConsumption() {
        return energyConsumption;
    }
}

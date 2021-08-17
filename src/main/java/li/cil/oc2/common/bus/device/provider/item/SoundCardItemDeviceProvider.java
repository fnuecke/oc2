package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.item.SoundCardItemDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.Items;
import li.cil.oc2.common.util.LocationSupplierUtils;

import java.util.Optional;

public class SoundCardItemDeviceProvider extends AbstractItemDeviceProvider {
    public SoundCardItemDeviceProvider() {
        super(Items.SOUND_CARD);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return Optional.of(new SoundCardItemDevice(query.getItemStack(), LocationSupplierUtils.of(query)));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.soundCardEnergyPerTick;
    }
}

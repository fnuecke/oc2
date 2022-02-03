/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.item.RedstoneInterfaceCardItemDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class RedstoneInterfaceCardItemDeviceProvider extends AbstractItemDeviceProvider {
    public RedstoneInterfaceCardItemDeviceProvider() {
        super(Items.REDSTONE_INTERFACE_CARD);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerBlockEntity().map(blockEntity ->
            new RedstoneInterfaceCardItemDevice(query.getItemStack(), blockEntity));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.redstoneInterfaceCardEnergyPerTick;
    }
}

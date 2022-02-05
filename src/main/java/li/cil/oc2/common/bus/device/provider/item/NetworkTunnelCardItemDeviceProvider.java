/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.vm.item.NetworkTunnelDevice;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class NetworkTunnelCardItemDeviceProvider extends AbstractItemDeviceProvider {
    public NetworkTunnelCardItemDeviceProvider() {
        super(Items.NETWORK_TUNNEL_CARD);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return Optional.of(new NetworkTunnelDevice(query.getItemStack()));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.networkTunnelEnergyPerTick;
    }
}

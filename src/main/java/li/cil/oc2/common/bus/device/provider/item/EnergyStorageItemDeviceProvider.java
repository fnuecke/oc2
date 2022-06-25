/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.rpc.EnergyStorageDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Optional;

public final class EnergyStorageItemDeviceProvider extends AbstractItemStackCapabilityDeviceProvider<IEnergyStorage> {
    public EnergyStorageItemDeviceProvider() {
        super(Capabilities::energyStorage);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query, final IEnergyStorage value) {
        return Optional.of(new ObjectDevice(new EnergyStorageDevice(value)));
    }
}

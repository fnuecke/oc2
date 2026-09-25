/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.energy.EnergyHandler;

@RPCDeviceDescription(typeNames = {"energy_storage"}, description = """
    Provided by any energy storage connected through a [bus interface](../block/bus_interface.md), such as a [charger](../block/charger.md) or the batteries of other mods. Amounts are in the energy unit of the mod, the same one [robots](../item/robot.md) use.

    With several storages connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](../item/wrench.md), and find it by that name instead.""")
public final class EnergyHandlerDevice extends IdentityProxy<EnergyHandler> {
    public EnergyHandlerDevice(final EnergyHandler identity) {
        super(identity);
    }

    @Callback(description = "Gets how much energy is currently stored.",
        returnValueDescription = "the stored amount of energy.")
    public int getEnergyStored() {
        return (int) identity.getEnergyStored();
    }

    @Callback(description = "Gets how much energy can be stored at most.",
        returnValueDescription = "the capacity of the storage.")
    public int getMaxEnergyStored() {
        return (int) identity.getMaxEnergyStored();
    }

    @Callback(description = "Gets whether energy can be taken out of the storage.",
        returnValueDescription = "whether the storage allows extracting energy.")
    public boolean canExtractEnergy() {
        return identity.canExtract();
    }

    @Callback(description = "Gets whether energy can be put into the storage.",
        returnValueDescription = "whether the storage allows receiving energy.")
    public boolean canReceiveEnergy() {
        return identity.canReceive();
    }
}

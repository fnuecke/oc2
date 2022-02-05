/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraftforge.energy.IEnergyStorage;

import java.util.Collection;
import java.util.Collections;

public final class EnergyStorageDevice extends IdentityProxy<IEnergyStorage> implements NamedDevice {
    public EnergyStorageDevice(final IEnergyStorage identity) {
        super(identity);
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singleton("energy_storage");
    }

    @Callback
    public int getEnergyStored() {
        return identity.getEnergyStored();
    }

    @Callback
    public int getMaxEnergyStored() {
        return identity.getMaxEnergyStored();
    }

    @Callback
    public boolean canExtractEnergy() {
        return identity.canExtract();
    }

    @Callback
    public boolean canReceiveEnergy() {
        return identity.canReceive();
    }
}

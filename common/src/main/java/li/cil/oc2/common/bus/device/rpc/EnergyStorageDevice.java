/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.energy.EnergyStorage;

import java.util.Collection;
import java.util.Collections;

public final class EnergyStorageDevice extends IdentityProxy<EnergyStorage> implements NamedDevice {
    public EnergyStorageDevice(final EnergyStorage identity) {
        super(identity);
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singleton("energy_storage");
    }

    @Callback
    public int getEnergyStored() {
        return (int) identity.getEnergyStored();
    }

    @Callback
    public int getMaxEnergyStored() {
        return (int) identity.getMaxEnergyStored();
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

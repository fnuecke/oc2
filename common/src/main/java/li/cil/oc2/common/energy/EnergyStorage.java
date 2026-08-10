/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

public interface EnergyStorage {
    long receiveEnergy(long maxReceive, boolean simulate);

    long extractEnergy(long maxExtract, boolean simulate);

    long getEnergyStored();

    long getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}

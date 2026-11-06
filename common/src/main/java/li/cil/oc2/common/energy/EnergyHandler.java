/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

import li.cil.oc2.common.transaction.TransactionContext;

public interface EnergyHandler {
    long receiveEnergy(long maxReceive, TransactionContext transaction);

    long extractEnergy(long maxExtract, TransactionContext transaction);

    long getEnergyStored();

    long getMaxEnergyStored();

    boolean canExtract();

    boolean canReceive();
}

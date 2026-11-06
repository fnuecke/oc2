/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities;

import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.transaction.TransactionContext;

public record CachedEnergyHandler(CapabilityCache<EnergyHandler> cache) implements EnergyHandler {
    @Override
    public long receiveEnergy(final long maxReceive, final TransactionContext transaction) {
        return handler().receiveEnergy(maxReceive, transaction);
    }

    @Override
    public long extractEnergy(final long maxExtract, final TransactionContext transaction) {
        return handler().extractEnergy(maxExtract, transaction);
    }

    @Override
    public long getEnergyStored() {
        return handler().getEnergyStored();
    }

    @Override
    public long getMaxEnergyStored() {
        return handler().getMaxEnergyStored();
    }

    @Override
    public boolean canExtract() {
        return handler().canExtract();
    }

    @Override
    public boolean canReceive() {
        return handler().canReceive();
    }

    private EnergyHandler handler() {
        final EnergyHandler handler = cache.get();
        if (handler == null) {
            throw new IllegalStateException("energy storage is gone");
        }
        return handler;
    }
}

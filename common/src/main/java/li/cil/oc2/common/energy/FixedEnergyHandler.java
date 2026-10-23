/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FixedEnergyHandler implements EnergyHandler {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    private final int capacity;
    private int energy;

    // --------------------------------------------------------------------- //

    public FixedEnergyHandler(final int capacity) {
        this.capacity = capacity;
    }

    // --------------------------------------------------------------------- //

    @Override
    public long receiveEnergy(final long maxReceive, final boolean simulate) {
        if (maxReceive <= 0) {
            return 0;
        }

        final int received = (int) Math.min(capacity - energy, maxReceive);
        if (!simulate) {
            energy += received;
        }
        return received;
    }

    @Override
    public long extractEnergy(final long maxExtract, final boolean simulate) {
        if (maxExtract <= 0) {
            return 0;
        }

        final int extracted = (int) Math.min(energy, maxExtract);
        if (!simulate) {
            energy -= extracted;
        }
        return extracted;
    }

    @Override
    public long getEnergyStored() {
        return energy;
    }

    @Override
    public long getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return true;
    }

    @Override
    public boolean canReceive() {
        return true;
    }

    // --------------------------------------------------------------------- //

    public void serialize(final ValueOutput output) {
        output.putInt(STORED_TAG_NAME, energy);
        output.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
    }

    public void deserialize(final ValueInput input) {
        energy = Math.min(capacity, input.getIntOr(STORED_TAG_NAME, 0));
    }
}

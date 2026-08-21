/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

import net.minecraft.nbt.CompoundTag;

public final class FixedEnergyStorage implements EnergyStorage {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    private final int capacity;
    private int energy;

    // --------------------------------------------------------------------- //

    public FixedEnergyStorage(final int capacity) {
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

    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(STORED_TAG_NAME, energy);
        tag.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
        return tag;
    }

    public void deserializeNBT(final CompoundTag tag) {
        energy = Math.min(capacity, tag.getInt(STORED_TAG_NAME));
    }
}

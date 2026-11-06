/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

import li.cil.oc2.common.transaction.SnapshotJournal;
import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class FixedEnergyHandler implements EnergyHandler {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    private final int capacity;
    private int energy;
    private final SnapshotJournal journal = SnapshotJournal.create(() -> energy, value -> energy = value, () -> {});

    // --------------------------------------------------------------------- //

    public FixedEnergyHandler(final int capacity) {
        this.capacity = capacity;
    }

    // --------------------------------------------------------------------- //

    @Override
    public long receiveEnergy(final long maxReceive, final TransactionContext transaction) {
        final int received = (int) Math.min(capacity - energy, maxReceive);
        if (received <= 0) {
            return 0;
        }

        journal.updateSnapshots(transaction);
        energy += received;
        return received;
    }

    @Override
    public long extractEnergy(final long maxExtract, final TransactionContext transaction) {
        final int extracted = (int) Math.min(energy, maxExtract);
        if (extracted <= 0) {
            return 0;
        }

        journal.updateSnapshots(transaction);
        energy -= extracted;
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

    // Internal fast-path where we don't really need transactions.
    public boolean consume(final int amount) {
        if (amount > energy) {
            return false;
        }

        energy -= amount;
        return true;
    }

    public void serialize(final ValueOutput output) {
        output.putInt(STORED_TAG_NAME, energy);
        output.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
    }

    public void deserialize(final ValueInput input) {
        energy = Math.min(capacity, input.getIntOr(STORED_TAG_NAME, 0));
    }
}

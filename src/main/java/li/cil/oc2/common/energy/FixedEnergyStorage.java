/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.energy.EnergyStorage;

public final class FixedEnergyStorage extends EnergyStorage {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    public FixedEnergyStorage(final int capacity) {
        super(capacity);
    }

    @Override
    public CompoundTag serializeNBT() {
        final CompoundTag tag = new CompoundTag();
        tag.putInt(STORED_TAG_NAME, energy);
        tag.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
        return tag;
    }

    @Override
    public void deserializeNBT(final Tag tag) {
        if (tag instanceof final CompoundTag compoundTag) {
            energy = compoundTag.getInt(STORED_TAG_NAME);
        }
    }
}

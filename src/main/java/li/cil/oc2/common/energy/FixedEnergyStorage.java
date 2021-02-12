package li.cil.oc2.common.energy;

import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.energy.EnergyStorage;

public final class FixedEnergyStorage extends EnergyStorage implements INBTSerializable<CompoundNBT> {
    public static final String STORED_TAG_NAME = "stored";
    public static final String CAPACITY_TAG_NAME = "capacity";

    public FixedEnergyStorage(final int capacity) {
        super(capacity);
    }

    @Override
    public CompoundNBT serializeNBT() {
        final CompoundNBT tag = new CompoundNBT();
        tag.putInt(STORED_TAG_NAME, energy);
        tag.putInt(CAPACITY_TAG_NAME, capacity); // Mostly for tooltips.
        return tag;
    }

    @Override
    public void deserializeNBT(final CompoundNBT tag) {
        energy = tag.getInt(STORED_TAG_NAME);
    }
}

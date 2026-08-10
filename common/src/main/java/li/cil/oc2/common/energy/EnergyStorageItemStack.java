/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.energy;

import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.world.item.ItemStack;

public final class EnergyStorageItemStack implements EnergyStorage {
    private final ItemStack stack;
    private final int capacity;
    private final String[] tagPath;

    public EnergyStorageItemStack(final ItemStack stack, final int capacity, final String... tagPath) {
        this.stack = stack;
        this.capacity = capacity;
        this.tagPath = tagPath;
    }

    @Override
    public long receiveEnergy(final long maxReceive, final boolean simulate) {
        final long stored = getEnergyStored();
        final long receiveLimit = capacity - stored;
        final long receive = Math.min(maxReceive, receiveLimit);
        if (!simulate) {
            NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), tagPath)
                .putInt(FixedEnergyStorage.STORED_TAG_NAME, (int) (stored + receive));
        }
        return receive;
    }

    @Override
    public long extractEnergy(final long maxExtract, final boolean simulate) {
        return 0;
    }

    @Override
    public long getEnergyStored() {
        return NBTUtils.getChildTag(stack.getTag(), tagPath).getInt(FixedEnergyStorage.STORED_TAG_NAME);
    }

    @Override
    public long getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return false; // We don't want our items to be usable as batteries.
    }

    @Override
    public boolean canReceive() {
        return true;
    }
}

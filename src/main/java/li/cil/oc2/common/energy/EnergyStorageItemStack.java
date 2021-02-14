package li.cil.oc2.common.energy;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.util.NBTUtils;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Direction;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;

public final class EnergyStorageItemStack implements IEnergyStorage, ICapabilityProvider {
    private final LazyOptional<IEnergyStorage> optional = LazyOptional.of(() -> this);

    private final ItemStack stack;
    private final int capacity;
    private final String[] tagPath;

    public EnergyStorageItemStack(final ItemStack stack, final int capacity, final String... tagPath) {
        this.stack = stack;
        this.capacity = capacity;
        this.tagPath = tagPath;
    }

    @Override
    public int receiveEnergy(final int maxReceive, final boolean simulate) {
        final int stored = getEnergyStored();
        final int receiveLimit = capacity - stored;
        final int receive = Math.min(maxReceive, receiveLimit);
        if (!simulate) {
            NBTUtils.getOrCreateChildTag(stack.getOrCreateTag(), tagPath)
                    .putInt(FixedEnergyStorage.STORED_TAG_NAME, stored + receive);
        }
        return receive;
    }

    @Override
    public int extractEnergy(final int maxExtract, final boolean simulate) {
        return 0;
    }

    @Override
    public int getEnergyStored() {
        return NBTUtils.getChildTag(stack.getTag(), tagPath).getInt(FixedEnergyStorage.STORED_TAG_NAME);
    }

    @Override
    public int getMaxEnergyStored() {
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

    @Override
    public <T> LazyOptional<T> getCapability(final Capability<T> capability, @Nullable final Direction side) {
        if (Capabilities.ENERGY_STORAGE != null && capability != null) {
            return Capabilities.ENERGY_STORAGE.orEmpty(capability, optional);
        } else {
            return LazyOptional.empty();
        }
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.energy.EnergyStorage;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

public final class NeoForgeCapabilityAdapters {
    @Nullable
    public static EnergyStorage energy(@Nullable final IEnergyStorage storage) {
        return storage == null ? null : new EnergyStorageAdapter(storage);
    }

    @Nullable
    public static ItemHandler items(@Nullable final IItemHandler handler) {
        return handler == null ? null : new ItemHandlerAdapter(handler);
    }

    public static IItemHandler toNeoForge(final ItemHandler handler) {
        return new ReverseItemHandlerAdapter(handler);
    }

    public static IEnergyStorage toNeoForge(final EnergyStorage storage) {
        return new ReverseEnergyStorageAdapter(storage);
    }

    // ------------------------------------------------------------- //

    private record EnergyStorageAdapter(IEnergyStorage inner) implements EnergyStorage {
        @Override
        public long receiveEnergy(final long maxReceive, final boolean simulate) {
            return inner.receiveEnergy(clamp(maxReceive), simulate);
        }

        @Override
        public long extractEnergy(final long maxExtract, final boolean simulate) {
            return inner.extractEnergy(clamp(maxExtract), simulate);
        }

        @Override
        public long getEnergyStored() {
            return inner.getEnergyStored();
        }

        @Override
        public long getMaxEnergyStored() {
            return inner.getMaxEnergyStored();
        }

        @Override
        public boolean canExtract() {
            return inner.canExtract();
        }

        @Override
        public boolean canReceive() {
            return inner.canReceive();
        }
    }

    private record ReverseEnergyStorageAdapter(EnergyStorage inner) implements IEnergyStorage {
        @Override
        public int receiveEnergy(final int maxReceive, final boolean simulate) {
            return clamp(inner.receiveEnergy(maxReceive, simulate));
        }

        @Override
        public int extractEnergy(final int maxExtract, final boolean simulate) {
            return clamp(inner.extractEnergy(maxExtract, simulate));
        }

        @Override
        public int getEnergyStored() {
            return clamp(inner.getEnergyStored());
        }

        @Override
        public int getMaxEnergyStored() {
            return clamp(inner.getMaxEnergyStored());
        }

        @Override
        public boolean canExtract() {
            return inner.canExtract();
        }

        @Override
        public boolean canReceive() {
            return inner.canReceive();
        }
    }

    private record ItemHandlerAdapter(IItemHandler inner) implements ItemHandler {
        @Override
        public int getSlots() {
            return inner.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return inner.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return inner.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return inner.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return inner.getSlotLimit(slot);
        }
    }

    private record ReverseItemHandlerAdapter(ItemHandler inner) implements IItemHandler {
        @Override
        public int getSlots() {
            return inner.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            return inner.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            return inner.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            return inner.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(final int slot) {
            return inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }

            return inner.insertItem(slot, stack, true).getCount() < stack.getCount();
        }
    }

    private static int clamp(final long value) {
        return (int) Math.min(value, Integer.MAX_VALUE);
    }

    private NeoForgeCapabilityAdapters() {
    }
}

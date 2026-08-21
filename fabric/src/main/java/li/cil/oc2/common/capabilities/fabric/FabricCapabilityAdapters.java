/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import li.cil.oc2.api.inventory.ItemHandler;
import li.cil.oc2.common.energy.EnergyStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Bridges our loader-agnostic capability interfaces and Fabric's transaction-based ones.
 * <p>
 * Our model originates from Forge and (like now NeoForge) simulates operations and then repeats them
 * for real, while Fabric performs the operation inside a transaction that is committed or rolled back.
 * Adapting to Fabric is therefore a transaction per call; adapting from Fabric mutates and undoes the
 * change with the inverse operation if the transaction is aborted. It is what it is.
 */
public final class FabricCapabilityAdapters {
    @Nullable
    public static EnergyStorage energy(@Nullable final team.reborn.energy.api.EnergyStorage storage) {
        return storage == null ? null : new EnergyStorageAdapter(storage);
    }

    @Nullable
    public static ItemHandler items(@Nullable final Storage<ItemVariant> storage) {
        return storage == null ? null : new ItemHandlerAdapter(storage);
    }

    public static team.reborn.energy.api.EnergyStorage toFabric(final EnergyStorage storage) {
        return new ReverseEnergyStorageAdapter(storage);
    }

    public static Storage<ItemVariant> toFabric(final ItemHandler handler) {
        return new ReverseItemHandlerAdapter(handler);
    }

    @SuppressWarnings("deprecation")
    private static Transaction open() {
        return Transaction.openNested(Transaction.getCurrentUnsafe());
    }

    // --------------------------------------------------------------------- //

    private record EnergyStorageAdapter(team.reborn.energy.api.EnergyStorage inner) implements EnergyStorage {
        @Override
        public long receiveEnergy(final long maxReceive, final boolean simulate) {
            try (Transaction transaction = open()) {
                final long inserted = inner.insert(maxReceive, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted;
            }
        }

        @Override
        public long extractEnergy(final long maxExtract, final boolean simulate) {
            try (Transaction transaction = open()) {
                final long extracted = inner.extract(maxExtract, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return extracted;
            }
        }

        @Override
        public long getEnergyStored() {
            return inner.getAmount();
        }

        @Override
        public long getMaxEnergyStored() {
            return inner.getCapacity();
        }

        @Override
        public boolean canExtract() {
            return inner.supportsExtraction();
        }

        @Override
        public boolean canReceive() {
            return inner.supportsInsertion();
        }
    }

    private record ReverseEnergyStorageAdapter(EnergyStorage inner) implements team.reborn.energy.api.EnergyStorage {
        @Override
        public long insert(final long maxAmount, final TransactionContext transaction) {
            return new EnergyMovement(inner).insert(maxAmount, transaction);
        }

        @Override
        public long extract(final long maxAmount, final TransactionContext transaction) {
            return new EnergyMovement(inner).extract(maxAmount, transaction);
        }

        @Override
        public long getAmount() {
            return inner.getEnergyStored();
        }

        @Override
        public long getCapacity() {
            return inner.getMaxEnergyStored();
        }

        @Override
        public boolean supportsInsertion() {
            return inner.canReceive();
        }

        @Override
        public boolean supportsExtraction() {
            return inner.canExtract();
        }
    }

    private static final class EnergyMovement extends SnapshotParticipant<Long> {
        private final EnergyStorage inner;
        private long moved;

        private EnergyMovement(final EnergyStorage inner) {
            this.inner = inner;
        }

        long insert(final long maxAmount, final TransactionContext transaction) {
            updateSnapshots(transaction);
            final long inserted = inner.receiveEnergy(maxAmount, false);
            moved += inserted;
            return inserted;
        }

        long extract(final long maxAmount, final TransactionContext transaction) {
            updateSnapshots(transaction);
            final long extracted = inner.extractEnergy(maxAmount, false);
            moved -= extracted;
            return extracted;
        }

        @Override
        protected Long createSnapshot() {
            return moved;
        }

        @Override
        protected void readSnapshot(final Long snapshot) {
            final long delta = moved - snapshot;
            if (delta > 0) {
                inner.extractEnergy(delta, false);
            } else if (delta < 0) {
                inner.receiveEnergy(-delta, false);
            }
            moved = snapshot;
        }
    }

    // --------------------------------------------------------------------- //

    private record ItemHandlerAdapter(Storage<ItemVariant> inner) implements ItemHandler {
        @Override
        public int getSlots() {
            return slots().size();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            final SingleSlotStorage<ItemVariant> view = slot(slot);
            if (view == null) {
                return ItemStack.EMPTY;
            }

            final ItemVariant resource = view.getResource();
            if (resource.isBlank()) {
                return ItemStack.EMPTY;
            }

            return resource.toStack((int) Math.min(view.getAmount(), Integer.MAX_VALUE));
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            final SingleSlotStorage<ItemVariant> view = slot(slot);
            if (view == null) {
                return stack;
            }

            try (Transaction transaction = open()) {
                final long inserted = view.insert(ItemVariant.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }

                if (inserted >= stack.getCount()) {
                    return ItemStack.EMPTY;
                }

                return stack.copyWithCount(stack.getCount() - (int) inserted);
            }
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            final SingleSlotStorage<ItemVariant> view = slot(slot);
            if (view == null || amount <= 0) {
                return ItemStack.EMPTY;
            }

            final ItemVariant resource = view.getResource();
            if (resource.isBlank()) {
                return ItemStack.EMPTY;
            }

            try (Transaction transaction = open()) {
                final long extracted = view.extract(resource, amount, transaction);
                if (!simulate) {
                    transaction.commit();
                }

                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack((int) extracted);
            }
        }

        @Override
        public int getSlotLimit(final int slot) {
            final SingleSlotStorage<ItemVariant> view = slot(slot);
            if (view == null) {
                return 0;
            }

            return (int) Math.min(view.getCapacity(), Integer.MAX_VALUE);
        }

        @Nullable
        private SingleSlotStorage<ItemVariant> slot(final int slot) {
            final List<SingleSlotStorage<ItemVariant>> slots = slots();
            return slot >= 0 && slot < slots.size() ? slots.get(slot) : null;
        }

        private List<SingleSlotStorage<ItemVariant>> slots() {
            if (inner instanceof final SlottedStorage<ItemVariant> slotted) {
                return slotted.getSlots();
            }

            final List<SingleSlotStorage<ItemVariant>> slots = new ArrayList<>();
            for (final StorageView<ItemVariant> view : inner) {
                if (view instanceof final SingleSlotStorage<ItemVariant> single) {
                    slots.add(single);
                }
            }
            return slots;
        }
    }

    private static final class ReverseItemHandlerAdapter implements SlottedStorage<ItemVariant> {
        private final ItemHandler inner;
        private final List<SingleSlotStorage<ItemVariant>> slots = new ArrayList<>();

        private ReverseItemHandlerAdapter(final ItemHandler inner) {
            this.inner = inner;
            for (int slot = 0; slot < inner.getSlots(); slot++) {
                slots.add(new SlotView(inner, slot));
            }
        }

        @Override
        public int getSlotCount() {
            return slots.size();
        }

        @Override
        public SingleSlotStorage<ItemVariant> getSlot(final int slot) {
            return slots.get(slot);
        }

        @Override
        public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            long inserted = 0;
            for (final SingleSlotStorage<ItemVariant> slot : slots) {
                inserted += slot.insert(resource, maxAmount - inserted, transaction);
                if (inserted >= maxAmount) {
                    break;
                }
            }
            return inserted;
        }

        @Override
        public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            long extracted = 0;
            for (final SingleSlotStorage<ItemVariant> slot : slots) {
                extracted += slot.extract(resource, maxAmount - extracted, transaction);
                if (extracted >= maxAmount) {
                    break;
                }
            }
            return extracted;
        }

        @Override
        public java.util.Iterator<StorageView<ItemVariant>> iterator() {
            return new ArrayList<StorageView<ItemVariant>>(slots).iterator();
        }

        @Override
        public String toString() {
            return "ItemHandlerStorage[" + inner + "]";
        }
    }

    private static final class SlotView extends SnapshotParticipant<ItemStack> implements SingleSlotStorage<ItemVariant> {
        private final ItemHandler handler;
        private final int slot;

        private SlotView(final ItemHandler handler, final int slot) {
            this.handler = handler;
            this.slot = slot;
        }

        @Override
        public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) {
                return 0;
            }

            final int count = (int) Math.min(maxAmount, resource.getItem().getDefaultMaxStackSize());
            final ItemStack stack = resource.toStack(count);
            if (handler.insertItem(slot, stack, true).getCount() >= count) {
                return 0;
            }

            updateSnapshots(transaction);
            final ItemStack remainder = handler.insertItem(slot, stack, false);
            return count - remainder.getCount();
        }

        @Override
        public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) {
                return 0;
            }

            final ItemStack existing = handler.getStackInSlot(slot);
            if (existing.isEmpty() || !resource.matches(existing)) {
                return 0;
            }

            final int count = (int) Math.min(maxAmount, Integer.MAX_VALUE);
            if (handler.extractItem(slot, count, true).isEmpty()) {
                return 0;
            }

            updateSnapshots(transaction);
            return handler.extractItem(slot, count, false).getCount();
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public ItemVariant getResource() {
            return ItemVariant.of(handler.getStackInSlot(slot));
        }

        @Override
        public long getAmount() {
            return handler.getStackInSlot(slot).getCount();
        }

        @Override
        public long getCapacity() {
            return handler.getSlotLimit(slot);
        }

        @Override
        protected ItemStack createSnapshot() {
            return handler.getStackInSlot(slot).copy();
        }

        @Override
        protected void readSnapshot(final ItemStack snapshot) {
            handler.extractItem(slot, Integer.MAX_VALUE, false);
            if (!snapshot.isEmpty()) {
                handler.insertItem(slot, snapshot, false);
            }
        }
    }

    private FabricCapabilityAdapters() {
    }
}

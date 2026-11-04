/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import com.google.common.collect.MapMaker;
import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.DeferredNotifyItemHandler;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.util.ItemStackUtils;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.Transaction;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import javax.annotation.Nullable;
import java.util.Map;

public final class NeoForgeCapabilityAdapters {
    private static final Map<DeferredNotifyItemHandler, ReverseItemHandlerAdapter> ITEM_ADAPTERS = new MapMaker().weakKeys().weakValues().makeMap();
    private static final Map<FluidHandler, ReverseFluidHandlerAdapter> FLUID_ADAPTERS = new MapMaker().weakKeys().weakValues().makeMap();

    // --------------------------------------------------------------------- //

    @Nullable
    public static EnergyHandler energy(@Nullable final net.neoforged.neoforge.transfer.energy.EnergyHandler handler) {
        if (handler instanceof final ReverseEnergyHandlerAdapter reverse) {
            return reverse.inner;
        }
        return handler == null ? null : new EnergyHandlerAdapter(handler);
    }

    @Nullable
    public static ItemHandler items(@Nullable final ResourceHandler<ItemResource> handler) {
        if (handler instanceof final ReverseItemHandlerAdapter reverse) {
            return reverse.inner;
        }
        return handler == null ? null : new ItemHandlerAdapter(handler);
    }

    @Nullable
    public static FluidHandler fluids(@Nullable final ResourceHandler<FluidResource> handler) {
        if (handler instanceof final ReverseFluidHandlerAdapter reverse) {
            return reverse.inner;
        }
        return handler == null ? null : new FluidHandlerAdapter(handler);
    }

    public static ResourceHandler<ItemResource> toNeoForge(final DeferredNotifyItemHandler handler) {
        return adapter(handler);
    }

    public static net.neoforged.neoforge.transfer.energy.EnergyHandler toNeoForge(final EnergyHandler storage) {
        return new ReverseEnergyHandlerAdapter(storage);
    }

    public static ResourceHandler<FluidResource> toNeoForge(final FluidHandler handler) {
        return FLUID_ADAPTERS.computeIfAbsent(handler, ReverseFluidHandlerAdapter::new);
    }

    public static ItemAccess slot(final ItemHandler handler, final int slot) {
        if (handler instanceof ItemHandlerAdapter(ResourceHandler<ItemResource> inner)) {
            return ItemAccess.forHandlerIndexStrict(inner, slot);
        }
        return adapter((DeferredNotifyItemHandler) handler).access(slot);
    }

    public static ItemAccess stack(final ItemStack stack) {
        return new StackItemAccess(stack);
    }

    private static ReverseItemHandlerAdapter adapter(final DeferredNotifyItemHandler handler) {
        return ITEM_ADAPTERS.computeIfAbsent(handler, ReverseItemHandlerAdapter::new);
    }

    @SuppressWarnings("deprecation")
    private static Transaction open() {
        return Transaction.open(Transaction.getCurrentOpenedTransaction());
    }

    private static int clamp(final long value) {
        return Math.clamp(value, 0, Integer.MAX_VALUE);
    }

    // --------------------------------------------------------------------- //

    private record EnergyHandlerAdapter(net.neoforged.neoforge.transfer.energy.EnergyHandler inner) implements EnergyHandler {
        @Override
        public long receiveEnergy(final long maxReceive, final boolean simulate) {
            try (Transaction transaction = open()) {
                final int inserted = inner.insert(clamp(maxReceive), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return inserted;
            }
        }

        @Override
        public long extractEnergy(final long maxExtract, final boolean simulate) {
            try (Transaction transaction = open()) {
                final int extracted = inner.extract(clamp(maxExtract), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return extracted;
            }
        }

        @Override
        public long getEnergyStored() {
            return inner.getAmountAsLong();
        }

        @Override
        public long getMaxEnergyStored() {
            return inner.getCapacityAsLong();
        }

        @Override
        public boolean canExtract() {
            try (Transaction transaction = open()) {
                return inner.extract(1, transaction) > 0;
            }
        }

        @Override
        public boolean canReceive() {
            try (Transaction transaction = open()) {
                return inner.insert(1, transaction) > 0;
            }
        }
    }

    private static final class ReverseEnergyHandlerAdapter extends SnapshotJournal<Long>
        implements net.neoforged.neoforge.transfer.energy.EnergyHandler {
        private final EnergyHandler inner;
        private long moved;

        private ReverseEnergyHandlerAdapter(final EnergyHandler inner) {
            this.inner = inner;
        }

        @Override
        public long getAmountAsLong() {
            return inner.getEnergyStored();
        }

        @Override
        public long getCapacityAsLong() {
            return inner.getMaxEnergyStored();
        }

        @Override
        public int insert(final int amount, final TransactionContext transaction) {
            if (amount <= 0 || !inner.canReceive()) {
                return 0;
            }
            updateSnapshots(transaction);
            final int inserted = clamp(inner.receiveEnergy(amount, false));
            moved += inserted;
            return inserted;
        }

        @Override
        public int extract(final int amount, final TransactionContext transaction) {
            if (amount <= 0 || !inner.canExtract()) {
                return 0;
            }
            updateSnapshots(transaction);
            final int extracted = clamp(inner.extractEnergy(amount, false));
            moved -= extracted;
            return extracted;
        }

        @Override
        protected Long createSnapshot() {
            return moved;
        }

        @Override
        protected void revertToSnapshot(final Long snapshot) {
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

    private record ItemHandlerAdapter(ResourceHandler<ItemResource> inner) implements ItemHandler {
        @Override
        public int getSlots() {
            return inner.size();
        }

        @Override
        public ItemStack getStackInSlot(final int slot) {
            if (slot < 0 || slot >= inner.size()) {
                return ItemStack.EMPTY;
            }

            final ItemResource resource = inner.getResource(slot);
            if (resource.isEmpty()) {
                return ItemStack.EMPTY;
            }

            return resource.toStack(inner.getAmountAsInt(slot));
        }

        @Override
        public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (slot < 0 || slot >= inner.size()) {
                return stack;
            }

            try (Transaction transaction = open()) {
                final int inserted = inner.insert(slot, ItemResource.of(stack), stack.getCount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }

                if (inserted >= stack.getCount()) {
                    return ItemStack.EMPTY;
                }

                return stack.copyWithCount(stack.getCount() - inserted);
            }
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
            if (slot < 0 || slot >= inner.size() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            final ItemResource resource = inner.getResource(slot);
            if (resource.isEmpty()) {
                return ItemStack.EMPTY;
            }

            try (Transaction transaction = open()) {
                final int extracted = inner.extract(slot, resource, Math.min(amount, resource.getMaxStackSize()), transaction);
                if (!simulate) {
                    transaction.commit();
                }

                return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
            }
        }

        @Override
        public int getSlotLimit(final int slot) {
            if (slot < 0 || slot >= inner.size()) {
                return 0;
            }

            return inner.getCapacityAsInt(slot, inner.getResource(slot));
        }

        @Override
        public boolean isItemValid(final int slot, final ItemStack stack) {
            if (slot < 0 || slot >= inner.size()) {
                return false;
            }

            return inner.isValid(slot, ItemResource.of(stack));
        }
    }

    private static final class ReverseItemHandlerAdapter implements ResourceHandler<ItemResource> {
        private final DeferredNotifyItemHandler inner;
        private final SlotJournal[] slots;
        private final ItemAccess[] accesses;

        private ReverseItemHandlerAdapter(final DeferredNotifyItemHandler inner) {
            this.inner = inner;
            this.slots = new SlotJournal[inner.getSlots()];
            this.accesses = new ItemAccess[inner.getSlots()];
        }

        @Override
        public int size() {
            return slots.length;
        }

        @Override
        public ItemResource getResource(final int index) {
            return ItemResource.of(inner.getStackInSlot(index));
        }

        @Override
        public long getAmountAsLong(final int index) {
            return inner.getStackInSlot(index).getCount();
        }

        @Override
        public long getCapacityAsLong(final int index, final ItemResource resource) {
            final int limit = inner.getSlotLimit(index);
            return resource.isEmpty() ? limit : Math.min(limit, resource.getMaxStackSize());
        }

        @Override
        public boolean isValid(final int index, final ItemResource resource) {
            return !resource.isEmpty() && inner.isItemValid(index, resource.toStack());
        }

        @Override
        public int insert(final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            final int count = Math.min(amount, resource.getMaxStackSize());
            final int inserted = count - inner.insertItem(index, resource.toStack(count), true).getCount();
            if (inserted <= 0) {
                return 0;
            }

            journal(index).updateSnapshots(transaction);
            final ItemStack existing = inner.getStackInSlot(index);
            inner.setStackInSlotSilently(index, existing.isEmpty()
                ? resource.toStack(inserted)
                : existing.copyWithCount(existing.getCount() + inserted));
            return inserted;
        }

        @Override
        public int extract(final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            final ItemStack existing = inner.getStackInSlot(index);
            if (existing.isEmpty() || !resource.matches(existing)) {
                return 0;
            }

            final int extracted = inner.extractItem(index, amount, true).getCount();
            if (extracted <= 0) {
                return 0;
            }

            journal(index).updateSnapshots(transaction);
            inner.setStackInSlotSilently(index, existing.getCount() > extracted
                ? existing.copyWithCount(existing.getCount() - extracted)
                : ItemStack.EMPTY);
            return extracted;
        }

        @Override
        public String toString() {
            return "ItemHandlerResourceHandler[" + inner + "]";
        }

        private ItemAccess access(final int index) {
            if (accesses[index] == null) {
                accesses[index] = ItemAccess.forHandlerIndexStrict(this, index);
            }
            return accesses[index];
        }

        private SlotJournal journal(final int index) {
            if (slots[index] == null) {
                slots[index] = new SlotJournal(index);
            }
            return slots[index];
        }

        private final class SlotJournal extends SnapshotJournal<ItemStack> {
            private final int slot;

            private SlotJournal(final int slot) {
                this.slot = slot;
            }

            @Override
            protected ItemStack createSnapshot() {
                return inner.getStackInSlot(slot);
            }

            @Override
            protected void revertToSnapshot(final ItemStack snapshot) {
                inner.setStackInSlotSilently(slot, snapshot);
            }

            @Override
            protected void onRootCommit(final ItemStack originalState) {
                inner.notifySlotChanged(slot);
            }
        }
    }

    private static final class StackItemAccess extends SnapshotJournal<ItemStack> implements ItemAccess {
        private final ItemStack stack;
        private final Item item;
        private ItemStack current;

        private StackItemAccess(final ItemStack stack) {
            this.stack = stack;
            this.item = stack.getItem();
            this.current = stack;
        }

        @Override
        public ItemResource getResource() {
            return ItemResource.of(current);
        }

        @Override
        public int getAmount() {
            return current.getCount();
        }

        @Override
        public int insert(final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (!resource.is(item) || (!current.isEmpty() && !resource.matches(current))) {
                return 0;
            }

            final int inserted = Math.min(amount, stack.getCount() - current.getCount());
            if (inserted <= 0) {
                return 0;
            }

            updateSnapshots(transaction);
            current = resource.toStack(current.getCount() + inserted);
            return inserted;
        }

        @Override
        public int extract(final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (current.isEmpty() || !resource.matches(current)) {
                return 0;
            }

            final int extracted = Math.min(amount, current.getCount());
            if (extracted <= 0) {
                return 0;
            }

            updateSnapshots(transaction);
            current = current.copyWithCount(current.getCount() - extracted);
            return extracted;
        }

        @Override
        protected ItemStack createSnapshot() {
            return current;
        }

        @Override
        protected void revertToSnapshot(final ItemStack snapshot) {
            current = snapshot;
        }

        @Override
        protected void onRootCommit(final ItemStack originalState) {
            ItemStackUtils.setContents(stack, current);
            current = stack;
        }
    }

    // --------------------------------------------------------------------- //

    private record FluidHandlerAdapter(ResourceHandler<FluidResource> inner) implements FluidHandler {
        @Override
        public int getTanks() {
            return inner.size();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            if (tank < 0 || tank >= inner.size()) {
                return FluidStack.EMPTY;
            }

            final FluidResource resource = inner.getResource(tank);
            final int amount = inner.getAmountAsInt(tank);
            if (resource.isEmpty() || amount <= 0) {
                return FluidStack.EMPTY;
            }

            return fromNeoForge(resource, amount);
        }

        @Override
        public int getTankCapacity(final int tank) {
            if (tank < 0 || tank >= inner.size()) {
                return 0;
            }

            return inner.getCapacityAsInt(tank, inner.getResource(tank));
        }

        @Override
        public int fill(final FluidStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return 0;
            }

            try (Transaction transaction = open()) {
                final int filled = inner.insert(toNeoForge(stack), stack.amount(), transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return filled;
            }
        }

        @Override
        public FluidStack drain(final FluidStack stack, final boolean simulate) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }

            return drain(toNeoForge(stack), stack.amount(), simulate);
        }

        @Override
        public FluidStack drain(final int amount, final boolean simulate) {
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }

            for (int tank = 0; tank < inner.size(); tank++) {
                final FluidResource resource = inner.getResource(tank);
                if (!resource.isEmpty() && inner.getAmountAsLong(tank) > 0) {
                    return drain(resource, amount, simulate);
                }
            }

            return FluidStack.EMPTY;
        }

        private FluidStack drain(final FluidResource resource, final int amount, final boolean simulate) {
            try (Transaction transaction = open()) {
                final int drained = inner.extract(resource, amount, transaction);
                if (!simulate) {
                    transaction.commit();
                }
                return drained > 0 ? fromNeoForge(resource, drained) : FluidStack.EMPTY;
            }
        }

        private static FluidStack fromNeoForge(final FluidResource resource, final int amount) {
            return new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount);
        }

        private static FluidResource toNeoForge(final FluidStack stack) {
            return FluidResource.of(stack.fluid(), stack.components());
        }
    }

    private static final class ReverseFluidHandlerAdapter extends SnapshotJournal<FluidStack[]> implements ResourceHandler<FluidResource> {
        private final FluidHandler inner;

        private ReverseFluidHandlerAdapter(final FluidHandler inner) {
            this.inner = inner;
        }

        @Override
        public int size() {
            return inner.getTanks();
        }

        @Override
        public FluidResource getResource(final int index) {
            final FluidStack stack = inner.getFluidInTank(index);
            return stack.isEmpty() ? FluidResource.EMPTY : FluidResource.of(stack.fluid(), stack.components());
        }

        @Override
        public long getAmountAsLong(final int index) {
            return inner.getFluidInTank(index).amount();
        }

        @Override
        public long getCapacityAsLong(final int index, final FluidResource resource) {
            return inner.getTankCapacity(index);
        }

        @Override
        public boolean isValid(final int index, final FluidResource resource) {
            return !resource.isEmpty();
        }

        @Override
        public int insert(final int index, final FluidResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            final FluidStack stack = new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount);
            if (inner.fill(stack, true) <= 0) {
                return 0;
            }

            updateSnapshots(transaction);
            return inner.fill(stack, false);
        }

        @Override
        public int extract(final int index, final FluidResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            final FluidStack stack = new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount);
            if (inner.drain(stack, true).isEmpty()) {
                return 0;
            }

            updateSnapshots(transaction);
            return inner.drain(stack, false).amount();
        }

        @Override
        protected FluidStack[] createSnapshot() {
            final FluidStack[] tanks = new FluidStack[inner.getTanks()];
            for (int tank = 0; tank < tanks.length; tank++) {
                tanks[tank] = inner.getFluidInTank(tank);
            }
            return tanks;
        }

        @Override
        protected void revertToSnapshot(final FluidStack[] snapshot) {
            for (int tank = 0; tank < inner.getTanks(); tank++) {
                final FluidStack current = inner.getFluidInTank(tank);
                if (!current.isEmpty()) {
                    inner.drain(current, false);
                }
            }
            for (final FluidStack stack : snapshot) {
                if (!stack.isEmpty()) {
                    inner.fill(stack, false);
                }
            }
        }
    }

    private NeoForgeCapabilityAdapters() {
    }
}

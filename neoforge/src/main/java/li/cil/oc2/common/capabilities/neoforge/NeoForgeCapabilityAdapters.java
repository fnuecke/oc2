/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.transaction.neoforge.NeoForgeTransactionContext;
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

public final class NeoForgeCapabilityAdapters {
    @Nullable
    public static EnergyHandler energy(@Nullable final net.neoforged.neoforge.transfer.energy.EnergyHandler handler) {
        if (handler instanceof ReverseEnergyHandlerAdapter(final EnergyHandler inner)) {
            return inner;
        }
        return handler == null ? null : new EnergyHandlerAdapter(handler);
    }

    @Nullable
    public static ItemHandler items(@Nullable final ResourceHandler<ItemResource> handler) {
        if (handler instanceof ReverseItemHandlerAdapter(final ItemHandler inner)) {
            return inner;
        }
        return handler == null ? null : new ItemHandlerAdapter(handler);
    }

    @Nullable
    public static FluidHandler fluids(@Nullable final ResourceHandler<FluidResource> handler) {
        if (handler instanceof ReverseFluidHandlerAdapter(final FluidHandler inner)) {
            return inner;
        }
        return handler == null ? null : new FluidHandlerAdapter(handler);
    }

    public static ResourceHandler<ItemResource> toNeoForge(final ItemHandler handler) {
        return new ReverseItemHandlerAdapter(handler);
    }

    public static net.neoforged.neoforge.transfer.energy.EnergyHandler toNeoForge(final EnergyHandler storage) {
        return new ReverseEnergyHandlerAdapter(storage);
    }

    public static ResourceHandler<FluidResource> toNeoForge(final FluidHandler handler) {
        return new ReverseFluidHandlerAdapter(handler);
    }

    public static ItemAccess slot(final ItemHandler handler, final int slot) {
        if (handler instanceof ItemHandlerAdapter(ResourceHandler<ItemResource> inner)) {
            return ItemAccess.forHandlerIndexStrict(inner, slot);
        }
        return ItemAccess.forHandlerIndexStrict(new ReverseItemHandlerAdapter(handler), slot);
    }

    public static ItemAccess stack(final ItemStack stack) {
        return new StackItemAccess(stack);
    }

    private static int clamp(final long value) {
        return Math.clamp(value, 0, Integer.MAX_VALUE);
    }

    // --------------------------------------------------------------------- //

    private record EnergyHandlerAdapter(net.neoforged.neoforge.transfer.energy.EnergyHandler inner) implements EnergyHandler {
        @Override
        public long receiveEnergy(final long maxReceive, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            return inner.insert(clamp(maxReceive), NeoForgeTransactionContext.unwrap(transaction));
        }

        @Override
        public long extractEnergy(final long maxExtract, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            return inner.extract(clamp(maxExtract), NeoForgeTransactionContext.unwrap(transaction));
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
            try (Transaction transaction = Transaction.openRoot()) {
                return inner.extract(1, transaction) > 0;
            }
        }

        @Override
        public boolean canReceive() {
            try (Transaction transaction = Transaction.openRoot()) {
                return inner.insert(1, transaction) > 0;
            }
        }
    }

    private record ReverseEnergyHandlerAdapter(EnergyHandler inner) implements net.neoforged.neoforge.transfer.energy.EnergyHandler {
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
            return inner.canReceive() ? clamp(inner.receiveEnergy(amount, new NeoForgeTransactionContext(transaction))) : 0;
        }

        @Override
        public int extract(final int amount, final TransactionContext transaction) {
            return inner.canExtract() ? clamp(inner.extractEnergy(amount, new NeoForgeTransactionContext(transaction))) : 0;
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
        public ItemStack insertItem(final int slot, final ItemStack stack, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            if (slot < 0 || slot >= inner.size()) {
                return stack;
            }

            final int inserted = inner.insert(slot, ItemResource.of(stack), stack.getCount(), NeoForgeTransactionContext.unwrap(transaction));
            if (inserted >= stack.getCount()) {
                return ItemStack.EMPTY;
            }

            return stack.copyWithCount(stack.getCount() - inserted);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (slot < 0 || slot >= inner.size() || amount <= 0) {
                return ItemStack.EMPTY;
            }

            final ItemResource resource = inner.getResource(slot);
            if (resource.isEmpty()) {
                return ItemStack.EMPTY;
            }

            final int extracted = inner.extract(slot, resource, Math.min(amount, resource.getMaxStackSize()), NeoForgeTransactionContext.unwrap(transaction));
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack(extracted);
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

    private record ReverseItemHandlerAdapter(ItemHandler inner) implements ResourceHandler<ItemResource> {
        @Override
        public int size() {
            return inner.getSlots();
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
            return count - inner.insertItem(index, resource.toStack(count), new NeoForgeTransactionContext(transaction)).getCount();
        }

        @Override
        public int extract(final int index, final ItemResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0 || !resource.matches(inner.getStackInSlot(index))) {
                return 0;
            }

            return inner.extractItem(index, amount, new NeoForgeTransactionContext(transaction)).getCount();
        }

        @Override
        public String toString() {
            return "ItemHandlerResourceHandler[" + inner + "]";
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
        public int fill(final FluidStack stack, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (stack.isEmpty()) {
                return 0;
            }

            return inner.insert(toNeoForge(stack), stack.amount(), NeoForgeTransactionContext.unwrap(transaction));
        }

        @Override
        public FluidStack drain(final FluidStack stack, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }

            return drain(toNeoForge(stack), stack.amount(), transaction);
        }

        @Override
        public FluidStack drain(final int amount, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }

            for (int tank = 0; tank < inner.size(); tank++) {
                final FluidResource resource = inner.getResource(tank);
                if (!resource.isEmpty() && inner.getAmountAsLong(tank) > 0) {
                    return drain(resource, amount, transaction);
                }
            }

            return FluidStack.EMPTY;
        }

        private FluidStack drain(final FluidResource resource, final int amount, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            final int drained = inner.extract(resource, amount, NeoForgeTransactionContext.unwrap(transaction));
            return drained > 0 ? fromNeoForge(resource, drained) : FluidStack.EMPTY;
        }

        private static FluidStack fromNeoForge(final FluidResource resource, final int amount) {
            return new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount);
        }

        private static FluidResource toNeoForge(final FluidStack stack) {
            return FluidResource.of(stack.fluid(), stack.components());
        }
    }

    private record ReverseFluidHandlerAdapter(FluidHandler inner) implements ResourceHandler<FluidResource> {
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

            return inner.fill(new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount), new NeoForgeTransactionContext(transaction));
        }

        @Override
        public int extract(final int index, final FluidResource resource, final int amount, final TransactionContext transaction) {
            if (resource.isEmpty() || amount <= 0) {
                return 0;
            }

            return inner.drain(new FluidStack(resource.getFluid(), resource.getComponentsPatch(), amount), new NeoForgeTransactionContext(transaction)).amount();
        }
    }

    private NeoForgeCapabilityAdapters() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import li.cil.oc2.common.energy.EnergyHandler;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.transaction.fabric.FabricTransactionContext;
import li.cil.oc2.common.util.ItemStackUtils;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.item.base.SingleStackStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.SlottedStorage;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageUtil;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FabricCapabilityAdapters {
    private static final long DROPLETS_PER_MILLIBUCKET = FluidConstants.BUCKET / FluidHandler.BUCKET;

    // --------------------------------------------------------------------- //

    @Nullable
    public static EnergyHandler energy(@Nullable final team.reborn.energy.api.EnergyStorage storage) {
        if (storage instanceof ReverseEnergyStorageAdapter(final EnergyHandler inner)) {
            return inner;
        }
        return storage == null ? null : new EnergyHandlerAdapter(storage);
    }

    @Nullable
    public static ItemHandler items(@Nullable final Storage<ItemVariant> storage) {
        if (storage instanceof final ReverseItemHandlerAdapter reverse) {
            return reverse.inner;
        }
        return storage == null ? null : new ItemHandlerAdapter(storage);
    }

    @Nullable
    public static FluidHandler fluids(@Nullable final Storage<FluidVariant> storage) {
        if (storage instanceof ReverseFluidHandlerAdapter(final FluidHandler inner)) {
            return inner;
        }
        return storage == null ? null : new FluidHandlerAdapter(storage);
    }

    public static team.reborn.energy.api.EnergyStorage toFabric(final EnergyHandler storage) {
        return new ReverseEnergyStorageAdapter(storage);
    }

    public static Storage<ItemVariant> toFabric(final ItemHandler handler) {
        return new ReverseItemHandlerAdapter(handler);
    }

    public static Storage<FluidVariant> toFabric(final FluidHandler handler) {
        return new ReverseFluidHandlerAdapter(handler);
    }

    public static SingleSlotStorage<ItemVariant> slot(final ItemHandler handler, final int slot) {
        return handler instanceof final ItemHandlerAdapter adapter ? Objects.requireNonNull(adapter.slot(slot)) : new SlotView(handler, slot);
    }

    public static SingleSlotStorage<ItemVariant> stack(final ItemStack stack) {
        return new StackStorage(stack);
    }

    // --------------------------------------------------------------------- //

    private record EnergyHandlerAdapter(team.reborn.energy.api.EnergyStorage inner) implements EnergyHandler {
        @Override
        public long receiveEnergy(final long maxReceive, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            return inner.insert(maxReceive, FabricTransactionContext.unwrap(transaction));
        }

        @Override
        public long extractEnergy(final long maxExtract, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            return inner.extract(maxExtract, FabricTransactionContext.unwrap(transaction));
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

    private record ReverseEnergyStorageAdapter(EnergyHandler inner) implements team.reborn.energy.api.EnergyStorage {
        @Override
        public long insert(final long maxAmount, final TransactionContext transaction) {
            return inner.receiveEnergy(maxAmount, new FabricTransactionContext(transaction));
        }

        @Override
        public long extract(final long maxAmount, final TransactionContext transaction) {
            return inner.extractEnergy(maxAmount, new FabricTransactionContext(transaction));
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
        public ItemStack insertItem(final int slot, final ItemStack stack, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (stack.isEmpty()) {
                return ItemStack.EMPTY;
            }

            final SingleSlotStorage<ItemVariant> view = slot(slot);
            if (view == null) {
                return stack;
            }

            final long inserted = view.insert(ItemVariant.of(stack), stack.getCount(), FabricTransactionContext.unwrap(transaction));
            if (inserted >= stack.getCount()) {
                return ItemStack.EMPTY;
            }

            return stack.copyWithCount(stack.getCount() - (int) inserted);
        }

        @Override
        public ItemStack extractItem(final int slot, final int amount, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            final SingleSlotStorage<ItemVariant> view = slot(slot);
            if (view == null || amount <= 0) {
                return ItemStack.EMPTY;
            }

            final ItemVariant resource = view.getResource();
            if (resource.isBlank()) {
                return ItemStack.EMPTY;
            }

            final long extracted = view.extract(resource, Math.min(amount, maxStackSize(resource)), FabricTransactionContext.unwrap(transaction));
            return extracted <= 0 ? ItemStack.EMPTY : resource.toStack((int) extracted);
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

    private record SlotView(ItemHandler handler, int slot) implements SingleSlotStorage<ItemVariant> {
        @Override
        public long insert(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0) {
                return 0;
            }

            final int count = (int) Math.min(maxAmount, maxStackSize(resource));
            return count - handler.insertItem(slot, resource.toStack(count), new FabricTransactionContext(transaction)).getCount();
        }

        @Override
        public long extract(final ItemVariant resource, final long maxAmount, final TransactionContext transaction) {
            if (resource.isBlank() || maxAmount <= 0 || !resource.matches(handler.getStackInSlot(slot))) {
                return 0;
            }

            return handler.extractItem(slot, (int) Math.min(maxAmount, Integer.MAX_VALUE), new FabricTransactionContext(transaction)).getCount();
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
            final ItemStack stack = handler.getStackInSlot(slot);
            final int limit = handler.getSlotLimit(slot);
            return stack.isEmpty() ? limit : Math.min(limit, stack.getMaxStackSize());
        }
    }

    private static final class StackStorage extends SingleStackStorage {
        private final ItemStack stack;
        private final Item item;
        private ItemStack current;

        private StackStorage(final ItemStack stack) {
            this.stack = stack;
            this.item = stack.getItem();
            this.current = stack;
        }

        @Override
        protected ItemStack getStack() {
            return current;
        }

        @Override
        protected void setStack(final ItemStack stack) {
            current = stack;
        }

        @Override
        protected boolean canInsert(final ItemVariant variant) {
            return variant.isOf(item);
        }

        @Override
        protected int getCapacity(final ItemVariant variant) {
            return stack.getCount();
        }

        @Override
        protected void onFinalCommit() {
            ItemStackUtils.setContents(stack, current);
            current = stack;
        }
    }

    // --------------------------------------------------------------------- //

    private record FluidHandlerAdapter(Storage<FluidVariant> inner) implements FluidHandler {
        @Override
        public int getTanks() {
            return tanks().size();
        }

        @Override
        public FluidStack getFluidInTank(final int tank) {
            final StorageView<FluidVariant> view = tank(tank);
            if (view == null || view.isResourceBlank() || view.getAmount() <= 0) {
                return FluidStack.EMPTY;
            }

            return fromFabric(view.getResource(), millibuckets(view.getAmount()));
        }

        @Override
        public int getTankCapacity(final int tank) {
            final StorageView<FluidVariant> view = tank(tank);
            return view == null ? 0 : millibuckets(view.getCapacity());
        }

        @Override
        public int fill(final FluidStack stack, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (stack.isEmpty()) {
                return 0;
            }

            final FluidVariant resource = toFabric(stack);
            return transfer(stack.amount(), FabricTransactionContext.unwrap(transaction), (amount, nested) -> inner.insert(resource, amount, nested));
        }

        @Override
        public FluidStack drain(final FluidStack stack, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (stack.isEmpty()) {
                return FluidStack.EMPTY;
            }

            return drain(toFabric(stack), stack.amount(), FabricTransactionContext.unwrap(transaction));
        }

        @Override
        public FluidStack drain(final int amount, final li.cil.oc2.common.transaction.TransactionContext transaction) {
            if (amount <= 0) {
                return FluidStack.EMPTY;
            }

            final TransactionContext context = FabricTransactionContext.unwrap(transaction);
            final FluidVariant resource = StorageUtil.findExtractableResource(inner, context);
            return resource == null ? FluidStack.EMPTY : drain(resource, amount, context);
        }

        private FluidStack drain(final FluidVariant resource, final int amount, final TransactionContext transaction) {
            final int drained = transfer(amount, transaction, (droplets, nested) -> inner.extract(resource, droplets, nested));
            return drained > 0 ? fromFabric(resource, drained) : FluidStack.EMPTY;
        }

        private static int transfer(final int amount, final TransactionContext transaction, final Transfer transfer) {
            if (amount <= 0) {
                return 0;
            }

            try (Transaction nested = transaction.openNested()) {
                final int rounded;
                try (Transaction probe = nested.openNested()) {
                    rounded = millibuckets(transfer.apply(droplets(amount), probe));
                }
                if (rounded <= 0) {
                    return 0;
                }

                final long requested = droplets(rounded);
                if (transfer.apply(requested, nested) != requested) {
                    return 0;
                }

                nested.commit();
                return rounded;
            }
        }

        @Nullable
        private StorageView<FluidVariant> tank(final int tank) {
            final List<? extends StorageView<FluidVariant>> tanks = tanks();
            return tank >= 0 && tank < tanks.size() ? tanks.get(tank) : null;
        }

        private List<? extends StorageView<FluidVariant>> tanks() {
            if (inner instanceof final SlottedStorage<FluidVariant> slotted) {
                return slotted.getSlots();
            }

            final List<StorageView<FluidVariant>> tanks = new ArrayList<>();
            for (final StorageView<FluidVariant> view : inner) {
                tanks.add(view);
            }
            return tanks;
        }

        @FunctionalInterface
        private interface Transfer {
            long apply(long droplets, TransactionContext transaction);
        }
    }

    private record ReverseFluidHandlerAdapter(FluidHandler inner) implements Storage<FluidVariant> {
        @Override
        public long insert(final FluidVariant resource, final long maxAmount, final TransactionContext transaction) {
            final FluidStack stack = fromFabric(resource, millibuckets(maxAmount));
            return stack.isEmpty() ? 0 : droplets(inner.fill(stack, new FabricTransactionContext(transaction)));
        }

        @Override
        public long extract(final FluidVariant resource, final long maxAmount, final TransactionContext transaction) {
            final FluidStack stack = fromFabric(resource, millibuckets(maxAmount));
            return stack.isEmpty() ? 0 : droplets(inner.drain(stack, new FabricTransactionContext(transaction)).amount());
        }

        @Override
        public java.util.Iterator<StorageView<FluidVariant>> iterator() {
            final List<StorageView<FluidVariant>> tanks = new ArrayList<>();
            for (int tank = 0; tank < inner.getTanks(); tank++) {
                tanks.add(new TankView(this, tank));
            }
            return tanks.iterator();
        }
    }

    private record TankView(ReverseFluidHandlerAdapter storage, int tank) implements StorageView<FluidVariant> {
        @Override
        public long extract(final FluidVariant resource, final long maxAmount, final TransactionContext transaction) {
            return resource.equals(getResource()) ? storage.extract(resource, maxAmount, transaction) : 0;
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            final FluidStack stack = storage.inner().getFluidInTank(tank);
            return stack.isEmpty() ? FluidVariant.blank() : toFabric(stack);
        }

        @Override
        public long getAmount() {
            return droplets(storage.inner().getFluidInTank(tank).amount());
        }

        @Override
        public long getCapacity() {
            return droplets(storage.inner().getTankCapacity(tank));
        }
    }

    private static int maxStackSize(final ItemVariant resource) {
        return resource.getComponentMap().getOrDefault(DataComponents.MAX_STACK_SIZE, 1);
    }

    private static FluidStack fromFabric(final FluidVariant variant, final int millibuckets) {
        return new FluidStack(variant.getFluid(), variant.getComponents(), millibuckets);
    }

    private static FluidVariant toFabric(final FluidStack stack) {
        return FluidVariant.of(stack.fluid(), stack.components());
    }

    private static long droplets(final int millibuckets) {
        return millibuckets * DROPLETS_PER_MILLIBUCKET;
    }

    private static int millibuckets(final long droplets) {
        return (int) Math.min(droplets / DROPLETS_PER_MILLIBUCKET, Integer.MAX_VALUE);
    }

    private FabricCapabilityAdapters() {
    }
}

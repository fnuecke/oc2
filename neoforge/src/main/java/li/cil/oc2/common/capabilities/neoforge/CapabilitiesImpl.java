/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

public final class CapabilitiesImpl {
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final BlockEntity blockEntity, final CapabilityType<T> type, @Nullable final Direction side) {
        if (blockEntity.getLevel() == null) {
            return null;
        }

        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) NeoForgeCapabilityAdapters.energy(
                blockEntity.getLevel().getCapability(EnergyStorage.BLOCK, blockEntity.getBlockPos(), side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.items(
                blockEntity.getLevel().getCapability(ItemHandler.BLOCK, blockEntity.getBlockPos(), side));
        }

        return blockEntity.getLevel().getCapability(block(type), blockEntity.getBlockPos(), side);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final ItemStack stack, final CapabilityType<T> type) {
        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) NeoForgeCapabilityAdapters.energy(stack.getCapability(EnergyStorage.ITEM));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.items(stack.getCapability(ItemHandler.ITEM));
        }

        return stack.getCapability(item(type));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final Entity entity, final CapabilityType<T> type, @Nullable final Direction side) {
        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) NeoForgeCapabilityAdapters.energy(entity.getCapability(EnergyStorage.ENTITY, side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            IItemHandler handler = entity.getCapability(ItemHandler.ENTITY_AUTOMATION, side);
            if (handler == null) {
                handler = entity.getCapability(ItemHandler.ENTITY);
            }
            return (T) NeoForgeCapabilityAdapters.items(handler);
        }

        return entity.getCapability(CapabilitiesImpl.entity(type), side);
    }

    @SuppressWarnings("unchecked")
    public static <T> Invalidatable<T> watch(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side,
                                             final CapabilityType<T> type) {
        if (!(level instanceof final ServerLevel serverLevel)) {
            final BlockEntity blockEntity = level.getBlockEntity(pos);
            final T value = blockEntity != null ? get(blockEntity, type, side) : null;
            return value != null ? Invalidatable.of(value) : Invalidatable.empty();
        }

        // The interop capabilities are watched through the platform's own capability, with the adapter applied
        // to whatever comes back out of it.
        if (type == Capabilities.ENERGY_STORAGE) {
            return (Invalidatable<T>) watch(serverLevel, pos, side, EnergyStorage.BLOCK,
                NeoForgeCapabilityAdapters::energy);
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (Invalidatable<T>) watch(serverLevel, pos, side, ItemHandler.BLOCK,
                NeoForgeCapabilityAdapters::items);
        }

        return watch(serverLevel, pos, side, block(type), Function.identity());
    }

    public static void invalidate(final BlockEntity blockEntity) {
        blockEntity.invalidateCapabilities();
    }

    // --------------------------------------------------------------------- //

    public static <T> BlockCapability<T, Direction> block(final CapabilityType<T> type) {
        return BlockCapability.createSided(type.id(), type.type());
    }

    public static <T> ItemCapability<T, Void> item(final CapabilityType<T> type) {
        return ItemCapability.createVoid(type.id(), type.type());
    }

    public static <T> EntityCapability<T, Direction> entity(final CapabilityType<T> type) {
        return EntityCapability.createSided(type.id(), type.type());
    }

    // --------------------------------------------------------------------- //

    private static <TCapability, T> Invalidatable<T> watch(final ServerLevel level, final BlockPos pos, @Nullable final Direction side,
                                                           final BlockCapability<TCapability, Direction> capability,
                                                           final Function<TCapability, T> adapter) {
        if (level.getCapability(capability, pos, side) == null) {
            return Invalidatable.empty();
        }

        final Watcher<T> watcher = new Watcher<>();
        final BlockCapabilityCache<TCapability, Direction> cache =
            BlockCapabilityCache.create(capability, level, pos, side, watcher, watcher);

        // The cache only fires its invalidation listener after it has been queried at least once...
        final TCapability value = cache.getCapability();
        if (value == null) {
            return Invalidatable.empty();
        }

        return watcher.watch(adapter.apply(value), cache);
    }

    private static final class Watcher<T> implements BooleanSupplier, Runnable {
        @Nullable
        private Invalidatable<T> value;
        @Nullable
        private BlockCapabilityCache<?, Direction> cache;

        Invalidatable<T> watch(final T value, final BlockCapabilityCache<?, Direction> cache) {
            this.value = Invalidatable.of(value);
            this.value.addListener(this::release); // value keeps us alive
            this.cache = cache; // we keep the cache alive, neoforge doesn't
            return this.value;
        }

        @Override
        public boolean getAsBoolean() {
            return cache != null;
        }

        @Override
        public void run() {
            if (value != null) {
                value.invalidate();
            }
        }

        private void release(final Invalidatable<T> unused) {
            cache = null;
        }
    }

    // --------------------------------------------------------------------- //

    private CapabilitiesImpl() {
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityCache;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.BlockCapabilityCache;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.FluidHandler;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Function;

public final class CapabilitiesImpl {
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final Level level, final BlockPos pos, final CapabilityType<T> type, @Nullable final Direction side) {
        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) NeoForgeCapabilityAdapters.energy(level.getCapability(EnergyStorage.BLOCK, pos, side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.items(level.getCapability(ItemHandler.BLOCK, pos, side));
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.fluids(level.getCapability(FluidHandler.BLOCK, pos, side));
        }

        return level.getCapability(block(type), pos, side);
    }

    @Nullable
    public static <T> T get(final BlockEntity blockEntity, final CapabilityType<T> type, @Nullable final Direction side) {
        if (blockEntity.getLevel() == null) {
            return null;
        }

        return get(blockEntity.getLevel(), blockEntity.getBlockPos(), type, side);
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

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.fluids(stack.getCapability(FluidHandler.ITEM));
        }

        return stack.getCapability(item(type));
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final li.cil.oc2.common.inventory.ItemHandler handler, final int slot, final CapabilityType<T> type) {
        final ItemStack stack = handler.getStackInSlot(slot);
        if (stack.getCount() != 1) {
            return null;
        }

        if (type == Capabilities.ENERGY_STORAGE || type == Capabilities.ITEM_HANDLER) {
            return get(stack, type);
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.fluids(stack.getCapability(FluidHandler.ITEM), handler, slot);
        }

        throw new IllegalArgumentException("unsupported capability: " + type);
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

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) NeoForgeCapabilityAdapters.fluids(entity.getCapability(FluidHandler.ENTITY, side));
        }

        return entity.getCapability(CapabilitiesImpl.entity(type), side);
    }

    public static <T> Optional<CapabilityCache<T>> cache(final ServerLevel level, final BlockPos pos, @Nullable final Direction side,
                                                         final CapabilityType<T> type) {
        if (type == Capabilities.ENERGY_STORAGE) {
            return cache(level, pos, side, type, EnergyStorage.BLOCK, NeoForgeCapabilityAdapters::energy);
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return cache(level, pos, side, type, ItemHandler.BLOCK, NeoForgeCapabilityAdapters::items);
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return cache(level, pos, side, type, FluidHandler.BLOCK, NeoForgeCapabilityAdapters::fluids);
        }

        return cache(level, pos, side, type, block(type), Function.identity());
    }

    public static Capabilities.InvalidationHandle listen(final ServerLevel level, final BlockPos pos, final Runnable callback) {
        // Invalidation is per position, so any capability works for listening.
        final Handle result = new Handle(callback);
        result.cache = BlockCapabilityCache.create(block(Capabilities.DEVICE_BUS_ELEMENT), level, pos,
            null, () -> result.callback != null, result::run);

        // The cache only fires its invalidation listener after it has been queried.
        result.cache.getCapability();

        return result;
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

    @SuppressWarnings("unchecked")
    private static <TCapability, T> Optional<CapabilityCache<T>> cache(final ServerLevel level, final BlockPos pos, @Nullable final Direction side,
                                                                       final CapabilityType<T> type,
                                                                       final BlockCapability<TCapability, Direction> capability,
                                                                       final Function<TCapability, ?> adapter) {
        final BlockCapabilityCache<TCapability, Direction> cache = BlockCapabilityCache.create(capability, level, pos, side);
        if (cache.getCapability() == null) {
            return Optional.empty();
        }

        return Optional.of(new CapabilityCache<>(level, pos, side, type, () -> {
            final TCapability value = cache.getCapability();
            return value != null ? (T) adapter.apply(value) : null;
        }));
    }

    // --------------------------------------------------------------------- //

    private static final class Handle implements Capabilities.InvalidationHandle {
        @Nullable
        private Runnable callback;
        @Nullable
        private BlockCapabilityCache<?, Direction> cache; // neoforge only holds it weakly

        Handle(final Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void drop() {
            callback = null;
            cache = null;
        }

        private void run() {
            final Runnable callback = this.callback;
            if (callback != null) {
                drop();
                callback.run();
            }
        }
    }
}

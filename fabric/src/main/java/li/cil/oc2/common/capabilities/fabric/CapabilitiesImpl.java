/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import li.cil.oc2.api.fabric.EnergyStorage;
import li.cil.oc2.api.fabric.FluidStorage;
import li.cil.oc2.api.fabric.ItemStorage;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityCache;
import li.cil.oc2.common.capabilities.CapabilityType;
import li.cil.oc2.common.inventory.ItemHandler;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiCache;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

public final class CapabilitiesImpl {
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final Level level, final BlockPos pos, final CapabilityType<T> type, @Nullable final Direction side) {
        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) FabricCapabilityAdapters.energy(EnergyStorage.SIDED.find(level, pos, side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) FabricCapabilityAdapters.items(ItemStorage.SIDED.find(level, pos, side));
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) FabricCapabilityAdapters.fluids(FluidStorage.SIDED.find(level, pos, side));
        }

        return block(type).find(level, pos, side);
    }

    @Nullable
    public static <T> T get(final BlockEntity blockEntity, final CapabilityType<T> type, @Nullable final Direction side) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }

        return get(level, blockEntity.getBlockPos(), type, side);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final ItemStack stack, final CapabilityType<T> type) {
        final ContainerItemContext context = ContainerItemContext.withConstant(stack);

        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) FabricCapabilityAdapters.energy(EnergyStorage.ITEM.find(stack, context));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) FabricCapabilityAdapters.items(ItemStorage.ITEM.find(stack, context));
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) FabricCapabilityAdapters.fluids(FluidStorage.ITEM.find(stack, context));
        }

        return item(type).find(stack, null);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final ItemHandler handler, final int slot, final CapabilityType<T> type) {
        final ContainerItemContext context = ContainerItemContext.ofSingleSlot(FabricCapabilityAdapters.slot(handler, slot));

        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) FabricCapabilityAdapters.energy(context.find(EnergyStorage.ITEM));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) FabricCapabilityAdapters.items(context.find(ItemStorage.ITEM));
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) FabricCapabilityAdapters.fluids(context.find(FluidStorage.ITEM));
        }

        throw new IllegalArgumentException("unsupported capability: " + type);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final Entity entity, final CapabilityType<T> type, @Nullable final Direction side) {
        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) FabricCapabilityAdapters.energy(EnergyStorage.ENTITY.find(entity, side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            final Storage<ItemVariant> storage = ItemStorage.ENTITY.find(entity, side);
            if (storage != null) {
                return (T) FabricCapabilityAdapters.items(storage);
            }

            // Fabric has no entity lookup that foreign mods register with, so anything that is simply a
            // Container, vanilla's inventory entities included, would otherwise be invisible here.
            if (entity instanceof final Container container) {
                return (T) FabricCapabilityAdapters.items(InventoryStorage.of(container, side));
            }

            return null;
        }

        if (type == Capabilities.FLUID_HANDLER) {
            return (T) FabricCapabilityAdapters.fluids(FluidStorage.ENTITY.find(entity, side));
        }

        return entity(type).find(entity, side);
    }

    public static <T> Optional<CapabilityCache<T>> cache(final ServerLevel level, final BlockPos pos, @Nullable final Direction side,
                                                         final CapabilityType<T> type) {
        // Unlike NeoForge's cache, BlockApiCache loads the chunk when queried.
        final Supplier<T> source = source(level, pos, side, type);
        final CapabilityCache<T> cache = new CapabilityCache<>(level, pos, side, type,
            () -> level.isLoaded(pos) ? source.get() : null);
        if (cache.get() == null) {
            return Optional.empty();
        }

        return Optional.of(cache);
    }

    public static Capabilities.InvalidationHandle listen(final ServerLevel level, final BlockPos pos, final Runnable callback) {
        return CapabilityWatchers.listen(level, pos, callback);
    }

    public static void invalidate(final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level != null) {
            CapabilityWatchers.invalidate(level, blockEntity.getBlockPos());
        }
    }

    // --------------------------------------------------------------------- //

    public static <T> BlockApiLookup<T, Direction> block(final CapabilityType<T> type) {
        return BlockApiLookup.get(type.id(), type.type(), Direction.class);
    }

    public static <T> ItemApiLookup<T, Void> item(final CapabilityType<T> type) {
        return ItemApiLookup.get(type.id(), type.type(), Void.class);
    }

    public static <T> EntityApiLookup<T, Direction> entity(final CapabilityType<T> type) {
        return EntityApiLookup.get(type.id(), type.type(), Direction.class);
    }

    // --------------------------------------------------------------------- //

    @SuppressWarnings("unchecked")
    private static <T> Supplier<T> source(final ServerLevel level, final BlockPos pos, @Nullable final Direction side, final CapabilityType<T> type) {
        if (type == Capabilities.ENERGY_STORAGE) {
            final BlockApiCache<team.reborn.energy.api.EnergyStorage, Direction> cache = BlockApiCache.create(EnergyStorage.SIDED, level, pos);
            return () -> (T) FabricCapabilityAdapters.energy(cache.find(side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            final BlockApiCache<Storage<ItemVariant>, Direction> cache = BlockApiCache.create(ItemStorage.SIDED, level, pos);
            return () -> (T) FabricCapabilityAdapters.items(cache.find(side));
        }

        if (type == Capabilities.FLUID_HANDLER) {
            final BlockApiCache<Storage<FluidVariant>, Direction> cache = BlockApiCache.create(FluidStorage.SIDED, level, pos);
            return () -> (T) FabricCapabilityAdapters.fluids(cache.find(side));
        }

        final BlockApiCache<T, Direction> cache = BlockApiCache.create(block(type), level, pos);
        return () -> cache.find(side);
    }

    private CapabilitiesImpl() {
    }
}

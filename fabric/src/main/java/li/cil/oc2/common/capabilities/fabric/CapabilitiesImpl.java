/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.fabric;

import li.cil.oc2.api.fabric.EnergyStorage;
import li.cil.oc2.api.fabric.ItemStorage;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.lookup.v1.entity.EntityApiLookup;
import net.fabricmc.fabric.api.lookup.v1.item.ItemApiLookup;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;

public final class CapabilitiesImpl {
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final BlockEntity blockEntity, final CapabilityType<T> type, @Nullable final Direction side) {
        final Level level = blockEntity.getLevel();
        if (level == null) {
            return null;
        }

        final BlockPos pos = blockEntity.getBlockPos();

        if (type == Capabilities.ENERGY_STORAGE) {
            return (T) FabricCapabilityAdapters.energy(EnergyStorage.SIDED.find(level, pos, side));
        }

        if (type == Capabilities.ITEM_HANDLER) {
            return (T) FabricCapabilityAdapters.items(ItemStorage.SIDED.find(level, pos, side));
        }

        return block(type).find(level, pos, side);
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

        return item(type).find(stack, null);
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

        return entity(type).find(entity, side);
    }

    public static <T> Invalidatable<T> watch(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side,
                                             final CapabilityType<T> type) {
        final BlockEntity blockEntity = level.getBlockEntity(pos);
        final T value = blockEntity != null ? get(blockEntity, type, side) : null;
        if (value == null) {
            return Invalidatable.empty();
        }

        return CapabilityWatchers.watch(level, pos, value);
    }

    public static void invalidate(final BlockEntity blockEntity) {
        final Level level = blockEntity.getLevel();
        if (level != null) {
            CapabilityWatchers.invalidate(level, blockEntity.getBlockPos());
        }
    }

    // ------------------------------------------------------------- //

    public static <T> BlockApiLookup<T, Direction> block(final CapabilityType<T> type) {
        return BlockApiLookup.get(type.id(), type.type(), Direction.class);
    }

    public static <T> ItemApiLookup<T, Void> item(final CapabilityType<T> type) {
        return ItemApiLookup.get(type.id(), type.type(), Void.class);
    }

    public static <T> EntityApiLookup<T, Direction> entity(final CapabilityType<T> type) {
        return EntityApiLookup.get(type.id(), type.type(), Direction.class);
    }

    private CapabilitiesImpl() {
    }
}

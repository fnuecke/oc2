/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.capabilities.neoforge;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.BlockCapability;
import net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage;
import net.neoforged.neoforge.capabilities.Capabilities.ItemHandler;
import net.neoforged.neoforge.capabilities.EntityCapability;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.items.IItemHandler;

import javax.annotation.Nullable;

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

    public static void invalidate(final BlockEntity blockEntity) {
        blockEntity.invalidateCapabilities();
    }

    // ------------------------------------------------------------- //

    public static <T> BlockCapability<T, Direction> block(final CapabilityType<T> type) {
        return BlockCapability.createSided(type.id(), type.type());
    }

    public static <T> ItemCapability<T, Void> item(final CapabilityType<T> type) {
        return ItemCapability.createVoid(type.id(), type.type());
    }

    public static <T> EntityCapability<T, Direction> entity(final CapabilityType<T> type) {
        return EntityCapability.createSided(type.id(), type.type());
    }

    private CapabilitiesImpl() {
    }
}

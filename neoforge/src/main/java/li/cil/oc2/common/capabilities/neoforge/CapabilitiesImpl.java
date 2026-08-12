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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

public final class CapabilitiesImpl {
    private static final Map<CapabilityType<?>, BlockCapability<?, Direction>> BLOCK = new HashMap<>();
    private static final Map<CapabilityType<?>, ItemCapability<?, Void>> ITEM = new HashMap<>();
    private static final Map<CapabilityType<?>, EntityCapability<?, Direction>> ENTITY = new HashMap<>();

    // ------------------------------------------------------------- //

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

        final BlockCapability<T, Direction> capability = (BlockCapability<T, Direction>) BLOCK.get(type);
        if (capability == null) {
            return null;
        }

        return blockEntity.getLevel().getCapability(capability, blockEntity.getBlockPos(), side);
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

        final ItemCapability<T, Void> capability = (ItemCapability<T, Void>) ITEM.get(type);
        if (capability == null) {
            return null;
        }

        return stack.getCapability(capability);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(final Entity entity, final CapabilityType<T> type, @Nullable final Direction side) {
        final EntityCapability<T, Direction> capability = (EntityCapability<T, Direction>) ENTITY.get(type);
        if (capability == null) {
            return null;
        }

        return entity.getCapability(capability, side);
    }

    public static void invalidate(final BlockEntity blockEntity) {
        blockEntity.invalidateCapabilities();
    }

    // ------------------------------------------------------------- //

    @SuppressWarnings("unchecked")
    static <T> BlockCapability<T, Direction> block(final CapabilityType<T> type) {
        return (BlockCapability<T, Direction>) BLOCK.computeIfAbsent(type,
            t -> BlockCapability.createSided(t.id(), t.type()));
    }

    @SuppressWarnings("unchecked")
    static <T> ItemCapability<T, Void> item(final CapabilityType<T> type) {
        return (ItemCapability<T, Void>) ITEM.computeIfAbsent(type,
            t -> ItemCapability.createVoid(t.id(), t.type()));
    }

    @SuppressWarnings("unchecked")
    static <T> EntityCapability<T, Direction> entity(final CapabilityType<T> type) {
        return (EntityCapability<T, Direction>) ENTITY.computeIfAbsent(type,
            t -> EntityCapability.createSided(t.id(), t.type()));
    }

    private CapabilitiesImpl() {
    }
}

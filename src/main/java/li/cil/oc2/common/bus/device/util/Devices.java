/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.Providers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public final class Devices {
    public static BlockDeviceQuery makeQuery(final BlockEntity blockEntity, @Nullable final Direction side) {
        final Level level = requireNonNull(blockEntity.getLevel());
        final BlockPos pos = blockEntity.getBlockPos();
        return new BlockQuery(level, pos, side);
    }

    public static BlockDeviceQuery makeQuery(final LevelAccessor level, final BlockPos pos, @Nullable final Direction side) {
        return new BlockQuery(level, pos, side);
    }

    public static ItemDeviceQuery makeQuery(final ItemStack stack) {
        return new ItemQuery(stack);
    }

    public static ItemDeviceQuery makeQuery(final BlockEntity blockEntity, final ItemStack stack) {
        return new ItemQuery(blockEntity, stack);
    }

    public static ItemDeviceQuery makeQuery(final Entity entity, final ItemStack stack) {
        return new ItemQuery(entity, stack);
    }

    public static Optional<List<Invalidatable<BlockDeviceInfo>>> getDevices(final BlockDeviceQuery query) {
        final ChunkPos queryChunk = new ChunkPos(query.getQueryPosition());
        if (!query.getLevel().hasChunk(queryChunk.x, queryChunk.z)) {
            return Optional.empty();
        }

        final IForgeRegistry<BlockDeviceProvider> registry = Providers.blockDeviceProviderRegistry();
        final ArrayList<Invalidatable<BlockDeviceInfo>> devices = new ArrayList<>();
        for (final BlockDeviceProvider provider : registry.getValues()) {
            final Invalidatable<Device> device = provider.getDevice(query);
            if (device.isPresent()) {
                devices.add(device.mapWithDependency(d -> new BlockDeviceInfo(provider, d)));
            }
        }

        return Optional.of(devices);
    }

    public static List<ItemDeviceInfo> getDevices(final ItemDeviceQuery query) {
        if (query.getItemStack().isEmpty()) {
            return Collections.emptyList();
        }

        final IForgeRegistry<ItemDeviceProvider> registry = Providers.itemDeviceProviderRegistry();
        final ArrayList<ItemDeviceInfo> devices = new ArrayList<>();
        for (final ItemDeviceProvider provider : registry.getValues()) {
            final Optional<ItemDevice> device = provider.getDevice(query);
            device.ifPresent(d -> devices.add(new ItemDeviceInfo(provider, d, provider.getEnergyConsumption(query))));
        }
        return devices;
    }

    public static int getEnergyConsumption(final ItemDeviceQuery query) {
        if (query.getItemStack().isEmpty()) {
            return 0;
        }

        final IForgeRegistry<ItemDeviceProvider> registry = Providers.itemDeviceProviderRegistry();
        long accumulator = 0;
        for (final ItemDeviceProvider provider : registry.getValues()) {
            accumulator += Math.max(0, provider.getEnergyConsumption(query));
        }
        if (accumulator > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) accumulator;
        }
    }

    ///////////////////////////////////////////////////////////////////

    private record BlockQuery(LevelAccessor level, BlockPos pos, @Nullable Direction side) implements BlockDeviceQuery {
        @Override
        public LevelAccessor getLevel() {
            return level;
        }

        @Override
        public BlockPos getQueryPosition() {
            return pos;
        }

        @Nullable
        @Override
        public Direction getQuerySide() {
            return side;
        }
    }

    private record ItemQuery(
        @Nullable BlockEntity blockEntity,
        @Nullable Entity entity,
        ItemStack stack
    ) implements ItemDeviceQuery {
        public ItemQuery(final ItemStack stack) {
            this(null, null, stack);
        }

        public ItemQuery(final BlockEntity blockEntity, final ItemStack stack) {
            this(blockEntity, null, stack);
        }

        public ItemQuery(final Entity entity, final ItemStack stack) {
            this(null, entity, stack);
        }

        @Override
        public Optional<BlockEntity> getContainerBlockEntity() {
            return Optional.ofNullable(blockEntity);
        }

        @Override
        public Optional<Entity> getContainerEntity() {
            return Optional.ofNullable(entity);
        }

        @Override
        public ItemStack getItemStack() {
            return stack;
        }
    }
}

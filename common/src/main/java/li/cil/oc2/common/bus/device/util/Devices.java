/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import dev.architectury.registry.registries.Registrar;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.api.bus.device.vm.ArchitectureType;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.Providers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class Devices {
    public static BlockDeviceQuery makeQuery(@Nullable final ArchitectureType architectureType, final LevelAccessor level, final BlockPos pos, @Nullable final Direction side) {
        return new BlockQuery(architectureType, level, pos, side);
    }

    public static ItemDeviceQuery makeQuery(final ItemStack stack) {
        return new ItemQuery(null, null, null, stack);
    }

    public static ItemDeviceQuery makeQuery(@Nullable final ArchitectureType architectureType, final BlockEntity blockEntity, final ItemStack stack) {
        return new ItemQuery(architectureType, blockEntity, null, stack);
    }

    public static ItemDeviceQuery makeQuery(@Nullable final ArchitectureType architectureType, final Entity entity, final ItemStack stack) {
        return new ItemQuery(architectureType, null, entity, stack);
    }

    public static Optional<List<Invalidatable<BlockDeviceInfo>>> getDevices(final BlockDeviceQuery query) {
        final ChunkPos queryChunk = new ChunkPos(query.getQueryPosition());
        if (!query.getLevel().hasChunk(queryChunk.x, queryChunk.z)) {
            return Optional.empty();
        }

        final Registrar<BlockDeviceProvider> registry = Providers.blockDeviceProviderRegistry();
        final ArrayList<Invalidatable<BlockDeviceInfo>> devices = new ArrayList<>();
        for (final BlockDeviceProvider provider : registry) {
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

        final Registrar<ItemDeviceProvider> registry = Providers.itemDeviceProviderRegistry();
        final ArrayList<ItemDeviceInfo> devices = new ArrayList<>();
        for (final ItemDeviceProvider provider : registry) {
            final Optional<ItemDevice> device = provider.getDevice(query);
            device.ifPresent(d -> devices.add(new ItemDeviceInfo(provider, d, provider.getEnergyConsumption(query))));
        }
        return devices;
    }

    public static int getEnergyConsumption(final ItemDeviceQuery query) {
        if (query.getItemStack().isEmpty()) {
            return 0;
        }

        final Registrar<ItemDeviceProvider> registry = Providers.itemDeviceProviderRegistry();
        long accumulator = 0;
        for (final ItemDeviceProvider provider : registry) {
            accumulator += Math.max(0, provider.getEnergyConsumption(query));
        }
        if (accumulator > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        } else {
            return (int) accumulator;
        }
    }

    // --------------------------------------------------------------------- //

    private record BlockQuery(
        @Nullable ArchitectureType architectureType,
        LevelAccessor level,
        BlockPos pos,
        @Nullable Direction side
    ) implements BlockDeviceQuery {
        @Override
        public Optional<ArchitectureType> getArchitectureType() {
            return Optional.ofNullable(architectureType);
        }

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
        @Nullable ArchitectureType architectureType,
        @Nullable BlockEntity blockEntity,
        @Nullable Entity entity,
        ItemStack stack
    ) implements ItemDeviceQuery {
        @Override
        public Optional<ArchitectureType> getArchitectureType() {
            return Optional.ofNullable(architectureType);
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

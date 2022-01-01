package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.Providers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.*;

import static java.util.Objects.requireNonNull;

public final class Devices {
    public static BlockDeviceQuery makeQuery(final BlockEntity blockEntity, @Nullable final Direction side) {
        final Level level = requireNonNull(blockEntity.getLevel());
        final BlockPos pos = blockEntity.getBlockPos();
        return new BlockQuery(level, pos, side);
    }

    public static BlockDeviceQuery makeQuery(final Level level, final BlockPos pos, @Nullable final Direction side) {
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

    public static List<LazyOptional<BlockDeviceInfo>> getDevices(final BlockDeviceQuery query) {
        if (!query.getLevel().isLoaded(query.getQueryPosition())) {
            return Collections.emptyList();
        }

        final IForgeRegistry<BlockDeviceProvider> registry = Providers.BLOCK_DEVICE_PROVIDER_REGISTRY.get();
        final ArrayList<LazyOptional<BlockDeviceInfo>> devices = new ArrayList<>();
        for (final BlockDeviceProvider provider : registry.getValues()) {
            final LazyOptional<Device> device = provider.getDevice(query);
            if (device.isPresent()) {
                final LazyOptional<BlockDeviceInfo> info = device.lazyMap(d -> new BlockDeviceInfo(provider, d));
                device.addListener(unused -> info.invalidate());
                devices.add(info);
            }
        }
        return devices;
    }

    public static List<ItemDeviceInfo> getDevices(final ItemDeviceQuery query) {
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY.get();
        final ArrayList<ItemDeviceInfo> devices = new ArrayList<>();
        for (final ItemDeviceProvider provider : registry.getValues()) {
            final Optional<ItemDevice> device = provider.getDevice(query);
            device.ifPresent(d -> devices.add(new ItemDeviceInfo(provider, d, provider.getEnergyConsumption(query))));
        }
        return devices;
    }

    public static Collection<DeviceType> getDeviceTypes(final ItemDeviceQuery query) {
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY.get();
        final HashSet<DeviceType> deviceTypes = new HashSet<>();
        for (final ItemDeviceProvider provider : registry.getValues()) {
            final Optional<DeviceType> device = provider.getDeviceType(query);
            device.ifPresent(deviceTypes::add);
        }
        return deviceTypes;
    }

    public static int getEnergyConsumption(final ItemDeviceQuery query) {
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY.get();
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

    private static class BlockQuery implements BlockDeviceQuery {
        private final Level level;
        private final BlockPos pos;
        @Nullable private final Direction side;

        public BlockQuery(final Level level, final BlockPos pos, @Nullable final Direction side) {
            this.level = level;
            this.pos = pos;
            this.side = side;
        }

        @Override
        public Level getLevel() {
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

    private static final class ItemQuery implements ItemDeviceQuery {
        @Nullable private final BlockEntity blockEntity;
        @Nullable private final Entity entity;
        private final ItemStack stack;

        public ItemQuery(final ItemStack stack) {
            blockEntity = null;
            entity = null;
            this.stack = stack;
        }

        public ItemQuery(final BlockEntity blockEntity, final ItemStack stack) {
            this.blockEntity = blockEntity;
            entity = null;
            this.stack = stack;
        }

        public ItemQuery(final Entity entity, final ItemStack stack) {
            blockEntity = null;
            this.entity = entity;
            this.stack = stack;
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

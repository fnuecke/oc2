package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.DeviceType;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.Providers;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.*;

public final class Devices {
    public static List<LazyOptional<BlockDeviceInfo>> getDevices(final TileEntity tileEntity, @Nullable final Direction side) {
        final World world = tileEntity.getWorld();
        final BlockPos pos = tileEntity.getPos();

        if (world == null) throw new IllegalArgumentException();

        return getDevices(world, pos, side);
    }

    public static List<LazyOptional<BlockDeviceInfo>> getDevices(final World world, final BlockPos pos, @Nullable final Direction side) {
        return getDevices(new BlockQuery(world, pos, side));
    }

    public static List<ItemDeviceInfo> getDevices(final ItemStack stack) {
        return getDevices(new ItemQuery(stack));
    }

    public static List<ItemDeviceInfo> getDevices(final TileEntity tileEntity, final ItemStack stack) {
        return getDevices(new ItemQuery(tileEntity, stack));
    }

    public static List<ItemDeviceInfo> getDevices(final Entity entity, final ItemStack stack) {
        return getDevices(new ItemQuery(entity, stack));
    }

    public static Collection<DeviceType> getDeviceTypes(final ItemStack stack) {
        return getDeviceTypes(new ItemQuery(stack));
    }

    ///////////////////////////////////////////////////////////////////

    private static List<LazyOptional<BlockDeviceInfo>> getDevices(final BlockQuery query) {
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

    private static List<ItemDeviceInfo> getDevices(final ItemQuery query) {
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY.get();
        final ArrayList<ItemDeviceInfo> devices = new ArrayList<>();
        for (final ItemDeviceProvider provider : registry.getValues()) {
            final Optional<ItemDevice> device = provider.getDevice(query);
            device.ifPresent(d -> devices.add(new ItemDeviceInfo(provider, d)));
        }
        return devices;
    }

    private static Collection<DeviceType> getDeviceTypes(final ItemQuery query) {
        final IForgeRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY.get();
        final HashSet<DeviceType> deviceTypes = new HashSet<>();
        for (final ItemDeviceProvider provider : registry.getValues()) {
            final Optional<DeviceType> device = provider.getDeviceType(query);
            device.ifPresent(deviceTypes::add);
        }
        return deviceTypes;
    }

    ///////////////////////////////////////////////////////////////////

    private static class BlockQuery implements BlockDeviceQuery {
        private final World world;
        private final BlockPos pos;
        @Nullable private final Direction side;

        public BlockQuery(final World world, final BlockPos pos, @Nullable final Direction side) {
            this.world = world;
            this.pos = pos;
            this.side = side;
        }

        @Override
        public World getWorld() {
            return world;
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
        @Nullable private final TileEntity tileEntity;
        @Nullable private final Entity entity;
        private final ItemStack stack;

        public ItemQuery(final ItemStack stack) {
            tileEntity = null;
            entity = null;
            this.stack = stack;
        }

        public ItemQuery(final TileEntity tileEntity, final ItemStack stack) {
            this.tileEntity = tileEntity;
            entity = null;
            this.stack = stack;
        }

        public ItemQuery(final Entity entity, final ItemStack stack) {
            tileEntity = null;
            this.entity = entity;
            this.stack = stack;
        }

        @Override
        public Optional<TileEntity> getContainerTileEntity() {
            return Optional.ofNullable(tileEntity);
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

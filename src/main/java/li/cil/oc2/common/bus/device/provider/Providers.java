package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.DeviceProvider;
import li.cil.oc2.api.bus.device.provider.DeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.BlockDeviceProvider;
import li.cil.oc2.common.bus.device.provider.util.TileEntityDeviceProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class Providers {
    private static final ArrayList<DeviceProvider> DEVICE_PROVIDERS = new ArrayList<>();

    ///////////////////////////////////////////////////////////////////

    public static void initialize() {
        addProvider(new EnergyStorageDeviceProvider());
        addProvider(new FluidHandlerDeviceProvider());
        addProvider(new ItemHandlerDeviceProvider());
        addProvider(new TileEntityDeviceProvider());
        addProvider(new BlockDeviceProvider());
        addProvider(new MemoryItemDeviceProvider());
        addProvider(new HardDriveItemDeviceProvider());
    }

    public static void addProvider(final DeviceProvider provider) {
        if (!DEVICE_PROVIDERS.contains(provider)) {
            DEVICE_PROVIDERS.add(provider);
        }
    }

    public static List<LazyOptional<Device>> getDevices(final TileEntity tileEntity, final Direction side) {
        final World world = tileEntity.getWorld();
        final BlockPos pos = tileEntity.getPos();

        if (world == null) throw new IllegalArgumentException();

        return getDevices(world, pos, side);
    }

    public static List<LazyOptional<Device>> getDevices(final World world, final BlockPos pos, final Direction side) {
        return getDevices(new BlockQuery(world, pos, side));
    }

    public static List<LazyOptional<Device>> getDevices(final ItemStack stack) {
        return getDevices(new ItemQuery(stack));
    }

    public static List<LazyOptional<Device>> getDevices(final DeviceQuery query) {
        final ArrayList<LazyOptional<Device>> devices = new ArrayList<>();
        for (final DeviceProvider provider : DEVICE_PROVIDERS) {
            final LazyOptional<Device> device = provider.getDevice(query);
            if (device.isPresent()) {
                devices.add(device);
            }
        }
        return devices;
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
        private final ItemStack stack;

        public ItemQuery(final ItemStack stack) {
            this.stack = stack;
        }

        @Override
        public ItemStack getItemStack() {
            return stack;
        }
    }
}

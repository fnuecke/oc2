package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.provider.DeviceProvider;
import li.cil.oc2.api.provider.DeviceQuery;
import li.cil.oc2.common.device.BlockDeviceQueryImpl;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;
import java.util.List;

public final class Providers {
    private static final ArrayList<DeviceProvider> DEVICE_PROVIDERS = new ArrayList<>();

    public static void initialize() {
        addProvider(new EnergyStorageDeviceProvider());
        addProvider(new FluidHandlerDeviceProvider());
        addProvider(new ItemHandlerDeviceProvider());
        addProvider(new TileEntityDeviceProvider());
        addProvider(new BlockDeviceProvider());
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
        return getDevices(new BlockDeviceQueryImpl(world, pos, side));
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
}

package li.cil.oc2.common.device;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.provider.DeviceProvider;
import li.cil.oc2.api.device.provider.DeviceQuery;
import li.cil.oc2.common.device.provider.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;

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

    public static LazyOptional<CompoundDevice> getDevice(final TileEntity tileEntity, final Direction side) {
        final World world = tileEntity.getWorld();
        final BlockPos pos = tileEntity.getPos();

        if (world == null) throw new IllegalArgumentException();

        return getDevice(world, pos, side);
    }

    public static LazyOptional<CompoundDevice> getDevice(final World world, final BlockPos pos, final Direction side) {
        return getDevice(new BlockDeviceQueryImpl(world, pos, side));
    }

    public static LazyOptional<CompoundDevice> getDevice(final DeviceQuery query) {
        final ArrayList<Device> devices = new ArrayList<>();
        final ArrayList<LazyOptional<Device>> optionals = new ArrayList<>();
        for (final DeviceProvider provider : DEVICE_PROVIDERS) {
            final LazyOptional<Device> optional = provider.getDevice(query);
            optional.ifPresent((device) -> {
                devices.add(device);
                optionals.add(optional);
            });
        }

        if (devices.isEmpty()) {
            return LazyOptional.empty();
        } else {
            final LazyOptional<CompoundDevice> compoundOptional = LazyOptional.of(() -> new CompoundDevice(devices));
            for (final LazyOptional<Device> optional : optionals) {
                optional.addListener((ignored) -> compoundOptional.invalidate());
            }
            return compoundOptional;
        }
    }
}

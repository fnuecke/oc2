package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.provider.DeviceInterfaceProvider;
import li.cil.oc2.api.device.provider.DeviceQuery;
import li.cil.oc2.common.device.BlockDeviceQueryImpl;
import li.cil.oc2.common.device.DeviceInterfaceCollection;
import li.cil.oc2.common.device.provider.*;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;

import java.util.ArrayList;

public final class Providers {
    private static final ArrayList<DeviceInterfaceProvider> DEVICE_PROVIDERS = new ArrayList<>();

    public static void initialize() {
        addProvider(new EnergyStorageDeviceInterfaceProvider());
        addProvider(new FluidHandlerDeviceInterfaceProvider());
        addProvider(new ItemHandlerDeviceInterfaceProvider());
        addProvider(new TileEntityDeviceInterfaceProvider());
        addProvider(new BlockDeviceInterfaceProvider());
    }

    public static void addProvider(final DeviceInterfaceProvider provider) {
        if (!DEVICE_PROVIDERS.contains(provider)) {
            DEVICE_PROVIDERS.add(provider);
        }
    }

    public static LazyOptional<DeviceInterfaceCollection> getDevice(final TileEntity tileEntity, final Direction side) {
        final World world = tileEntity.getWorld();
        final BlockPos pos = tileEntity.getPos();

        if (world == null) throw new IllegalArgumentException();

        return getDevice(world, pos, side);
    }

    public static LazyOptional<DeviceInterfaceCollection> getDevice(final World world, final BlockPos pos, final Direction side) {
        return getDevice(new BlockDeviceQueryImpl(world, pos, side));
    }

    public static LazyOptional<DeviceInterfaceCollection> getDevice(final DeviceQuery query) {
        final ArrayList<DeviceInterface> deviceInterfaces = new ArrayList<>();
        final ArrayList<LazyOptional<DeviceInterface>> optionals = new ArrayList<>();
        for (final DeviceInterfaceProvider provider : DEVICE_PROVIDERS) {
            final LazyOptional<DeviceInterface> optional = provider.getDeviceInterface(query);
            optional.ifPresent((device) -> {
                deviceInterfaces.add(device);
                optionals.add(optional);
            });
        }

        if (deviceInterfaces.isEmpty()) {
            return LazyOptional.empty();
        } else {
            final LazyOptional<DeviceInterfaceCollection> compoundOptional = LazyOptional.of(() -> new DeviceInterfaceCollection(deviceInterfaces));
            for (final LazyOptional<DeviceInterface> optional : optionals) {
                optional.addListener((ignored) -> compoundOptional.invalidate());
            }
            return compoundOptional;
        }
    }
}

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.DeviceProvider;
import li.cil.oc2.api.bus.device.provider.DeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.init.Providers;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class Devices {
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
        final IForgeRegistry<DeviceProvider> providers = Providers.PROVIDERS_REGISTRY.get();
        final ArrayList<LazyOptional<Device>> devices = new ArrayList<>();
        for (final DeviceProvider provider : providers.getValues()) {
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

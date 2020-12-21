package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.ItemDeviceProvider;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.init.Providers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class Devices {
    public static List<BlockDeviceInfo> getDevices(final BlockEntity tileEntity, final Direction side) {
        final World world = tileEntity.getWorld();
        final BlockPos pos = tileEntity.getPos();

        if (world == null) throw new IllegalArgumentException();

        return getDevices(world, pos, side);
    }

    public static List<BlockDeviceInfo> getDevices(final World world, final BlockPos pos, final Direction side) {
        return getDevices(new BlockQuery(world, pos, side));
    }

    public static List<ItemDeviceInfo> getDevices(final ItemStack stack) {
        return getDevices(new ItemQuery(stack));
    }

    ///////////////////////////////////////////////////////////////////

    private static List<BlockDeviceInfo> getDevices(final BlockQuery query) {
        final SimpleRegistry<BlockDeviceProvider> registry = Providers.BLOCK_DEVICE_PROVIDER_REGISTRY;
        final ArrayList<BlockDeviceInfo> devices = new ArrayList<>();
        for (final BlockDeviceProvider provider : registry) {
            final Optional<Device> device = provider.getDevice(query);
            device.ifPresent(d -> {
                devices.add(new BlockDeviceInfo(provider, d));
            });
        }
        return devices;
    }

    private static List<ItemDeviceInfo> getDevices(final ItemQuery query) {
        final SimpleRegistry<ItemDeviceProvider> registry = Providers.ITEM_DEVICE_PROVIDER_REGISTRY;
        final ArrayList<ItemDeviceInfo> devices = new ArrayList<>();
        for (final ItemDeviceProvider provider : registry) {
            final Optional<ItemDevice> device = provider.getDevice(query);
            device.ifPresent(d -> devices.add(new ItemDeviceInfo(provider, d)));
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

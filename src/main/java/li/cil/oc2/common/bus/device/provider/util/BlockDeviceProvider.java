package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.DeviceProvider;
import li.cil.oc2.api.bus.device.provider.DeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraftforge.common.util.LazyOptional;

public class BlockDeviceProvider implements DeviceProvider {
    @Override
    public LazyOptional<Device> getDevice(final DeviceQuery query) {
        if (!(query instanceof BlockDeviceQuery)) {
            return LazyOptional.empty();
        }

        final BlockDeviceQuery blockQuery = (BlockDeviceQuery) query;
        final BlockState blockState = blockQuery.getWorld().getBlockState(blockQuery.getQueryPosition());
        if (blockState.getBlock().isAir(blockState, blockQuery.getWorld(), blockQuery.getQueryPosition())) {
            return LazyOptional.empty();
        }

        final Block block = blockState.getBlock();
        if (!Callbacks.hasMethods(block)) {
            return LazyOptional.empty();
        }

        final String typeName = WorldUtils.getBlockName(blockQuery.getWorld(), blockQuery.getQueryPosition());
        return LazyOptional.of(() -> new ObjectDevice(block, typeName));
    }
}

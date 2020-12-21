package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public final class BlockStateDeviceProvider implements BlockDeviceProvider {
    @Override
    public Optional<Device> getDevice(final BlockDeviceQuery query) {
        final World world = query.getWorld();
        final BlockPos position = query.getQueryPosition();

        final BlockState blockState = world.getBlockState(position);
        final Block block = blockState.getBlock();

        if (!Callbacks.hasMethods(block)) {
            return Optional.empty();
        }

        final String typeName = WorldUtils.getBlockName(world, position);
        return Optional.of(new ObjectDevice(block, typeName));
    }
}

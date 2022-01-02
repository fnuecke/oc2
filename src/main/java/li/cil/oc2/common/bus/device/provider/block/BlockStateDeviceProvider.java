package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockStateDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        final Level level = query.getLevel();
        final BlockPos position = query.getQueryPosition();

        final BlockState blockState = level.getBlockState(position);

        if (blockState.isAir()) {
            return Invalidatable.empty();
        }

        final Block block = blockState.getBlock();
        if (!Callbacks.hasMethods(block)) {
            return Invalidatable.empty();
        }

        return Invalidatable.of(new ObjectDevice(block));
    }
}

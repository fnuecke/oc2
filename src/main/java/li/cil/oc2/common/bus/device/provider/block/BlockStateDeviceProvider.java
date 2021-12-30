package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.util.LazyOptional;

public final class BlockStateDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public LazyOptional<Device> getDevice(final BlockDeviceQuery query) {
        final Level world = query.getLevel();
        final BlockPos position = query.getQueryPosition();

        final BlockState blockState = world.getBlockState(position);

        if (blockState.isAir()) {
            return LazyOptional.empty();
        }

        final Block block = blockState.getBlock();
        if (!Callbacks.hasMethods(block)) {
            return LazyOptional.empty();
        }

        return LazyOptional.of(() -> new ObjectDevice(block));
    }
}

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class BlockStateDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Optional<Device> getDevice(final BlockDeviceQuery query) {
        final Level world = query.getLevel();
        final BlockPos position = query.getQueryPosition();

        final BlockState blockState = world.getBlockState(position);
        final Block block = blockState.getBlock();

        if (block.is(Blocks.AIR)) {
            return Optional.empty();
        }

        if (!Callbacks.hasMethods(block)) {
            return Optional.empty();
        }

        return Optional.of(new ObjectDevice(block));
    }
}

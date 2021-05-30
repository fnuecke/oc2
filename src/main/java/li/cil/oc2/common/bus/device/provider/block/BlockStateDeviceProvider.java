package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Optional;

public final class BlockStateDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Optional<Device> getDevice(final BlockDeviceQuery query) {
        final World world = query.getLevel();
        final BlockPos position = query.getQueryPosition();

        final BlockState blockState = world.getBlockState(position);
        final Block block = blockState.getBlock();

        if (block.isAir(blockState, world, position)) {
            return Optional.empty();
        }

        if (!Callbacks.hasMethods(block)) {
            return Optional.empty();
        }

        return Optional.of(() -> new ObjectDevice(block));
    }
}

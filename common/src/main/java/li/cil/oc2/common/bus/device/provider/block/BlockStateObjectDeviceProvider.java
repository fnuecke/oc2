/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

public final class BlockStateObjectDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Optional<Device> getDevice(final BlockDeviceQuery query) {
        final LevelAccessor level = query.getLevel();
        final BlockPos position = query.getQueryPosition();

        final BlockState blockState = level.getBlockState(position);

        if (blockState.isAir()) {
            return Optional.empty();
        }

        final Block block = blockState.getBlock();
        if (!Callbacks.hasMethods(block)) {
            return Optional.empty();
        }

        return Optional.of(new ObjectDevice(block));
    }
}

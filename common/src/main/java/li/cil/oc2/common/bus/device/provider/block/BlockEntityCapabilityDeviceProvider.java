/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class BlockEntityCapabilityDeviceProvider extends AbstractBlockEntityDeviceProvider<BlockEntity> {
    @Override
    protected Optional<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity blockEntity) {
        return Optional.ofNullable(Capabilities.get(blockEntity, Capabilities.DEVICE, query.getQuerySide()));
    }
}

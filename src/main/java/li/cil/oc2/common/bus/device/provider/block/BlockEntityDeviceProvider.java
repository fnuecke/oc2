/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class BlockEntityDeviceProvider extends AbstractBlockEntityDeviceProvider<BlockEntity> {
    @Override
    public Invalidatable<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity blockEntity) {
        if (Callbacks.hasMethods(blockEntity)) {
            return Invalidatable.of(new ObjectDevice(blockEntity));
        } else {
            return Invalidatable.empty();
        }
    }
}

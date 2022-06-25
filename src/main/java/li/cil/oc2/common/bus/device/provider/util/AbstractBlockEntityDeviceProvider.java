/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractBlockEntityDeviceProvider<T extends BlockEntity> extends AbstractBlockDeviceProvider {
    private final BlockEntityType<T> blockEntityType;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockEntityDeviceProvider(final BlockEntityType<T> blockEntityType) {
        this.blockEntityType = blockEntityType;
    }

    protected AbstractBlockEntityDeviceProvider() {
        this.blockEntityType = null;
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    @Override
    public final Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity blockEntity = query.getLevel().getBlockEntity(query.getQueryPosition());
        if (blockEntity == null) {
            return Invalidatable.empty();
        }

        if (blockEntityType != null && blockEntity.getType() != blockEntityType) {
            return Invalidatable.empty();
        }

        return getBlockDevice(query, (T) blockEntity);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract Invalidatable<Device> getBlockDevice(final BlockDeviceQuery query, final T blockEntity);
}

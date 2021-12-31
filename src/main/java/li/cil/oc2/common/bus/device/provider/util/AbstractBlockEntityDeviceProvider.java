package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.LevelUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.util.LazyOptional;

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
    public final LazyOptional<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity blockEntity = LevelUtils.getBlockEntityIfChunkExists(query.getLevel(), query.getQueryPosition());
        if (blockEntity == null) {
            return LazyOptional.empty();
        }

        if (blockEntityType != null && blockEntity.getType() != blockEntityType) {
            return LazyOptional.empty();
        }

        return getBlockDevice(query, (T) blockEntity);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final T blockEntity);
}

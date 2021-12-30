package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractTileEntityDeviceProvider<T extends BlockEntity> extends AbstractBlockDeviceProvider {
    private final BlockEntityType<T> tileEntityType;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntityDeviceProvider(final BlockEntityType<T> tileEntityType) {
        this.tileEntityType = tileEntityType;
    }

    protected AbstractTileEntityDeviceProvider() {
        this.tileEntityType = null;
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    @Override
    public final LazyOptional<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity tileEntity = WorldUtils.getBlockEntityIfChunkExists(query.getLevel(), query.getQueryPosition());
        if (tileEntity == null) {
            return LazyOptional.empty();
        }

        if (tileEntityType != null && tileEntity.getType() != tileEntityType) {
            return LazyOptional.empty();
        }

        return getBlockDevice(query, (T) tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final T tileEntity);
}

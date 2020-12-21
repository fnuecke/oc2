package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceProvider;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;

import java.util.Optional;

public abstract class AbstractTileEntityDeviceProvider<T extends BlockEntity> implements BlockDeviceProvider {
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
    public final Optional<Device> getDevice(final BlockDeviceQuery query) {
        final BlockEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(query.getWorld(), query.getQueryPosition());
        if (tileEntity == null) {
            return Optional.empty();
        }

        if (tileEntityType != null && tileEntity.getType() != tileEntityType) {
            return Optional.empty();
        }

        return getBlockDevice(query, (T) tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract Optional<Device> getBlockDevice(final BlockDeviceQuery query, final T tileEntity);
}

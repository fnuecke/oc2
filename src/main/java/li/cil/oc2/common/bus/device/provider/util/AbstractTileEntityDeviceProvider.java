package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractTileEntityDeviceProvider<T extends TileEntity> extends AbstractBlockDeviceProvider {
    private final TileEntityType<T> tileEntityType;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntityDeviceProvider(final TileEntityType<T> tileEntityType) {
        this.tileEntityType = tileEntityType;
    }

    protected AbstractTileEntityDeviceProvider() {
        this.tileEntityType = null;
    }

    ///////////////////////////////////////////////////////////////////

    @SuppressWarnings("unchecked")
    @Override
    public final LazyOptional<Device> getDevice(final BlockDeviceQuery query) {
        final TileEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(query.getWorld(), query.getQueryPosition());
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

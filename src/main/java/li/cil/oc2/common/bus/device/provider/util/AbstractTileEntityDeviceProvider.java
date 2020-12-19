package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.bus.device.provider.DeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractTileEntityDeviceProvider<T extends TileEntity> extends AbstractDeviceProvider {
    private final Class<T> tileEntityType;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntityDeviceProvider(final Class<T> tileEntityType) {
        this.tileEntityType = tileEntityType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LazyOptional<Device> getDevice(final DeviceQuery query) {
        if (!(query instanceof BlockDeviceQuery)) {
            return LazyOptional.empty();
        }

        final BlockDeviceQuery blockQuery = (BlockDeviceQuery) query;
        final TileEntity tileEntity = WorldUtils.getTileEntityIfChunkExists(blockQuery.getWorld(), blockQuery.getQueryPosition());
        if (!tileEntityType.isInstance(tileEntity)) {
            return LazyOptional.empty();
        }

        return getDevice(blockQuery, (T) tileEntity);
    }

    ///////////////////////////////////////////////////////////////////

    protected abstract LazyOptional<Device> getDevice(final BlockDeviceQuery query, final T tileEntity);
}

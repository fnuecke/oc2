package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.device.provider.DeviceProvider;
import li.cil.oc2.api.device.provider.DeviceQuery;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractTileEntityDeviceProvider<T extends TileEntity> implements DeviceProvider {
    private final Class<T> tileEntityType;

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
        final TileEntity tileEntity = blockQuery.getWorld().getTileEntity(blockQuery.getQueryPosition());
        if (!tileEntityType.isInstance(tileEntity)) {
            return LazyOptional.empty();
        }

        return getDevice(blockQuery, (T) tileEntity);
    }

    protected abstract LazyOptional<Device> getDevice(final BlockDeviceQuery query, final T tileEntity);
}

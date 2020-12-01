package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.device.provider.DeviceInterfaceProvider;
import li.cil.oc2.api.device.provider.DeviceQuery;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;

public abstract class AbstractTileEntityDeviceInterfaceProvider<T extends TileEntity> implements DeviceInterfaceProvider {
    private final Class<T> tileEntityType;

    protected AbstractTileEntityDeviceInterfaceProvider(final Class<T> tileEntityType) {
        this.tileEntityType = tileEntityType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LazyOptional<DeviceInterface> getDeviceInterface(final DeviceQuery query) {
        if (!(query instanceof BlockDeviceQuery)) {
            return LazyOptional.empty();
        }

        final BlockDeviceQuery blockQuery = (BlockDeviceQuery) query;
        final TileEntity tileEntity = blockQuery.getWorld().getTileEntity(blockQuery.getQueryPosition());
        if (!tileEntityType.isInstance(tileEntity)) {
            return LazyOptional.empty();
        }

        return getDeviceInterface(blockQuery, (T) tileEntity);
    }

    protected abstract LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery query, final T tileEntity);
}

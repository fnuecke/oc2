package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.DeviceInterface;
import li.cil.oc2.api.device.object.Callbacks;
import li.cil.oc2.api.device.object.ObjectDeviceInterface;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;

public final class TileEntityDeviceInterfaceProvider extends AbstractTileEntityDeviceInterfaceProvider<TileEntity> {
    public TileEntityDeviceInterfaceProvider() {
        super(TileEntity.class);
    }

    @Override
    public LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery query, final TileEntity tileEntity) {
        if (Callbacks.hasMethods(tileEntity)) {
            return LazyOptional.of(() -> {
                final String typeName = WorldUtils.getBlockName(query.getWorld(), query.getQueryPosition());
                return new ObjectDeviceInterface(tileEntity, typeName);
            });
        } else {
            return LazyOptional.empty();
        }
    }
}

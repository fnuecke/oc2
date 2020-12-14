package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;

public final class TileEntityDeviceProvider extends AbstractTileEntityDeviceProvider<TileEntity> {
    public TileEntityDeviceProvider() {
        super(TileEntity.class);
    }

    @Override
    public LazyOptional<Device> getDeviceInterface(final BlockDeviceQuery query, final TileEntity tileEntity) {
        if (Callbacks.hasMethods(tileEntity)) {
            return LazyOptional.of(() -> {
                final String typeName = WorldUtils.getBlockName(query.getWorld(), query.getQueryPosition());
                return new ObjectDevice(tileEntity, typeName);
            });
        } else {
            return LazyOptional.empty();
        }
    }
}

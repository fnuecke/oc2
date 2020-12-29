package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityDeviceProvider;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.util.LazyOptional;

public final class TileEntityDeviceProvider extends AbstractTileEntityDeviceProvider<TileEntity> {
    @Override
    public LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final TileEntity tileEntity) {
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

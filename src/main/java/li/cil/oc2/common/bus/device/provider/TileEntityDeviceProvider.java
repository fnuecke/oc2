package li.cil.oc2.common.bus.device.provider;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityDeviceProvider;
import li.cil.oc2.common.util.WorldUtils;
import net.minecraft.block.entity.BlockEntity;

import java.util.Optional;

public final class TileEntityDeviceProvider extends AbstractTileEntityDeviceProvider<BlockEntity> {
    @Override
    public Optional<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity tileEntity) {
        if (Callbacks.hasMethods(tileEntity)) {
            final String typeName = WorldUtils.getBlockName(query.getWorld(), query.getQueryPosition());
            return Optional.of(new ObjectDevice(tileEntity, typeName));
        } else {
            return Optional.empty();
        }
    }
}

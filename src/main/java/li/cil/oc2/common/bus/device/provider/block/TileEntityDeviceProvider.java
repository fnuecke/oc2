package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractTileEntityDeviceProvider;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.LazyOptional;

public final class TileEntityDeviceProvider extends AbstractTileEntityDeviceProvider<BlockEntity> {
    @Override
    public LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity tileEntity) {
        if (Callbacks.hasMethods(tileEntity)) {
            return LazyOptional.of(() -> new ObjectDevice(tileEntity));
        } else {
            return LazyOptional.empty();
        }
    }
}

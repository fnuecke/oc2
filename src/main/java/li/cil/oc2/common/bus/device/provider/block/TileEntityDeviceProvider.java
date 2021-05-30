package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.Callbacks;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockEntityDeviceProvider;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class BlockEntityDeviceProvider extends AbstractBlockEntityDeviceProvider<BlockEntity> {
    @Override
    public Optional<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity tileEntity) {
        if (Callbacks.hasMethods(tileEntity)) {
            return Optional.of(() -> new ObjectDevice(tileEntity));
        } else {
            return Optional.empty();
        }
    }
}

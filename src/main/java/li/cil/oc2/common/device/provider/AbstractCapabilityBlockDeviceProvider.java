package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.device.Device;
import li.cil.oc2.api.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.device.provider.DeviceProvider;
import li.cil.oc2.api.device.provider.DeviceQuery;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.function.Supplier;

public abstract class AbstractCapabilityBlockDeviceProvider<T> implements DeviceProvider {
    private final Supplier<Capability<T>> capabilitySupplier;

    protected AbstractCapabilityBlockDeviceProvider(final Supplier<Capability<T>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    public LazyOptional<Device> getDevice(final DeviceQuery query) {
        if (query instanceof BlockDeviceQuery) {
            final BlockDeviceQuery blockQuery = (BlockDeviceQuery) query;
            final TileEntity tileEntity = blockQuery.getWorld().getTileEntity(blockQuery.getQueryPosition());
            if (tileEntity != null) {
                final Capability<T> capability = capabilitySupplier.get();
                if (capability == null) throw new IllegalStateException();
                final LazyOptional<T> optional = tileEntity.getCapability(capability, blockQuery.getQuerySide());
                if (optional.isPresent()) {
                    return optional.map(this::getDevice);
                }
            }
        }
        return LazyOptional.empty();
    }

    protected abstract Device getDevice(final T value);
}

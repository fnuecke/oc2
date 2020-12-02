package li.cil.oc2.common.device.provider;

import li.cil.oc2.api.bus.device.DeviceInterface;
import li.cil.oc2.api.provider.BlockDeviceQuery;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.function.Supplier;

public abstract class AbstractCapabilityTileEntityDeviceInterfaceProvider<TCapability, TTileEntity extends TileEntity> extends AbstractTileEntityDeviceInterfaceProvider<TTileEntity> {
    private final Supplier<Capability<TCapability>> capabilitySupplier;

    protected AbstractCapabilityTileEntityDeviceInterfaceProvider(final Class<TTileEntity> tileEntityType, final Supplier<Capability<TCapability>> capabilitySupplier) {
        super(tileEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected final LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery blockQuery, final TileEntity tileEntity) {
        final Capability<TCapability> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final LazyOptional<TCapability> optional = tileEntity.getCapability(capability, blockQuery.getQuerySide());
        if (!optional.isPresent()) {
            return LazyOptional.empty();
        }

        final TCapability value = optional.orElseThrow(AssertionError::new);
        final LazyOptional<DeviceInterface> device = getDeviceInterface(blockQuery, value);
        optional.addListener(ignored -> device.invalidate());
        return device;
    }

    protected abstract LazyOptional<DeviceInterface> getDeviceInterface(final BlockDeviceQuery query, final TCapability value);
}

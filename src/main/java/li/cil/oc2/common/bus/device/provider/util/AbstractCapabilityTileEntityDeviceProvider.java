package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.function.Supplier;

public abstract class AbstractCapabilityTileEntityDeviceProvider<TCapability, TTileEntity extends TileEntity> extends AbstractTileEntityDeviceProvider<TTileEntity> {
    private final Supplier<Capability<TCapability>> capabilitySupplier;

    ///////////////////////////////////////////////////////////////////

    protected AbstractCapabilityTileEntityDeviceProvider(final Class<TTileEntity> tileEntityType, final Supplier<Capability<TCapability>> capabilitySupplier) {
        super(tileEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    @Override
    protected final LazyOptional<Device> getDeviceInterface(final BlockDeviceQuery blockQuery, final TileEntity tileEntity) {
        final Capability<TCapability> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final LazyOptional<TCapability> optional = tileEntity.getCapability(capability, blockQuery.getQuerySide());
        if (!optional.isPresent()) {
            return LazyOptional.empty();
        }

        final TCapability value = optional.orElseThrow(AssertionError::new);
        final LazyOptional<Device> device = getDeviceInterface(blockQuery, value);
        optional.addListener(ignored -> device.invalidate());
        return device;
    }

    protected abstract LazyOptional<Device> getDeviceInterface(final BlockDeviceQuery query, final TCapability value);
}

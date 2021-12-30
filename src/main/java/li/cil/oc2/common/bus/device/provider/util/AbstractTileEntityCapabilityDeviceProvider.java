package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.function.Supplier;

public abstract class AbstractTileEntityCapabilityDeviceProvider<TCapability, TTileEntity extends BlockEntity> extends AbstractTileEntityDeviceProvider<TTileEntity> {
    private final Supplier<Capability<TCapability>> capabilitySupplier;

    ///////////////////////////////////////////////////////////////////

    protected AbstractTileEntityCapabilityDeviceProvider(final BlockEntityType<TTileEntity> tileEntityType, final Supplier<Capability<TCapability>> capabilitySupplier) {
        super(tileEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    protected AbstractTileEntityCapabilityDeviceProvider(final Supplier<Capability<TCapability>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected final LazyOptional<Device> getBlockDevice(final BlockDeviceQuery blockQuery, final BlockEntity tileEntity) {
        final Capability<TCapability> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final LazyOptional<TCapability> optional = tileEntity.getCapability(capability, blockQuery.getQuerySide());
        if (!optional.isPresent()) {
            return LazyOptional.empty();
        }

        final TCapability value = optional.orElseThrow(AssertionError::new);
        final LazyOptional<Device> device = getBlockDevice(blockQuery, value);
        optional.addListener(ignored -> device.invalidate());
        return device;
    }

    protected abstract LazyOptional<Device> getBlockDevice(final BlockDeviceQuery query, final TCapability value);
}

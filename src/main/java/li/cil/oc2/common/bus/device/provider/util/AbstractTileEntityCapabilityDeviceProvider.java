package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.capabilities.Capability;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractBlockEntityCapabilityDeviceProvider<TCapability, TBlockEntity extends BlockEntity> extends AbstractBlockEntityDeviceProvider<TBlockEntity> {
    private final Supplier<Capability<TCapability>> capabilitySupplier;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockEntityCapabilityDeviceProvider(final BlockEntityType<TBlockEntity> tileEntityType, final Supplier<Capability<TCapability>> capabilitySupplier) {
        super(tileEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    protected AbstractBlockEntityCapabilityDeviceProvider(final Supplier<Capability<TCapability>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected final Optional<Device> getBlockDevice(final BlockDeviceQuery blockQuery, final BlockEntity tileEntity) {
        final Capability<TCapability> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final Optional<TCapability> optional = tileEntity.getCapability(capability, blockQuery.getQuerySide());
        if (!optional.isPresent()) {
            return Optional.empty();
        }

        final TCapability value = optional.orElseThrow(AssertionError::new);
        final Optional<Device> device = getBlockDevice(blockQuery, value);
        optional.addListener(ignored -> device.invalidate());
        return device;
    }

    protected abstract Optional<Device> getBlockDevice(final BlockDeviceQuery query, final TCapability value);
}

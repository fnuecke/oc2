package li.cil.oc2.common.bus.device.provider.util;

import alexiil.mc.lib.attributes.Attribute;
import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractBlockAttributeDeviceProvider<TAttribute, TBlockEntity extends BlockEntity> extends AbstractTileEntityDeviceProvider<TBlockEntity> {
    private final Supplier<Attribute<TAttribute>> capabilitySupplier;

    ///////////////////////////////////////////////////////////////////

    protected AbstractBlockAttributeDeviceProvider(final BlockEntityType<TBlockEntity> tileEntityType, final Supplier<Attribute<TAttribute>> capabilitySupplier) {
        super(tileEntityType);
        this.capabilitySupplier = capabilitySupplier;
    }

    protected AbstractBlockAttributeDeviceProvider(final Supplier<Attribute<TAttribute>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected final Optional<Device> getBlockDevice(final BlockDeviceQuery blockQuery, final BlockEntity tileEntity) {
        final Attribute<TAttribute> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();

        final TAttribute value = capability.getFirstOrNullFromNeighbour(tileEntity, blockQuery.getQuerySide());
        if (value == null) {
            return Optional.empty();
        }

        return getBlockDevice(blockQuery, value);
    }

    protected abstract Optional<Device> getBlockDevice(final BlockDeviceQuery query, final TAttribute value);
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

public abstract class AbstractBlockEntityCapabilityDeviceProvider<TCapability, TBlockEntity extends BlockEntity> extends AbstractBlockEntityDeviceProvider<TBlockEntity> {
    private final CapabilityType<TCapability> capability;

    // --------------------------------------------------------------------- //

    protected AbstractBlockEntityCapabilityDeviceProvider(final BlockEntityType<TBlockEntity> blockEntityType, final CapabilityType<TCapability> capability) {
        super(blockEntityType);
        this.capability = capability;
    }

    protected AbstractBlockEntityCapabilityDeviceProvider(final CapabilityType<TCapability> capability) {
        this.capability = capability;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected final Invalidatable<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity blockEntity) {
        final Invalidatable<TCapability> value = Capabilities.watch(
            query.getLevel(), blockEntity.getBlockPos(), query.getQuerySide(), capability);
        if (!value.isPresent()) {
            return Invalidatable.empty();
        }

        final Invalidatable<Device> device = getBlockDevice(query, value.get());
        final Invalidatable.ListenerToken token = value.addListener(unused -> device.invalidate());
        device.addListener(unused -> token.removeListener());

        return device;
    }

    protected abstract Invalidatable<Device> getBlockDevice(final BlockDeviceQuery query, final TCapability value);
}

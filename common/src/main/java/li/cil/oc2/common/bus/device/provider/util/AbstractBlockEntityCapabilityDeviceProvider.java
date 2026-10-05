/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.util;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityCache;
import li.cil.oc2.common.capabilities.CapabilityType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

import java.util.Optional;

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
    protected final Optional<Device> getBlockDevice(final BlockDeviceQuery query, final BlockEntity blockEntity) {
        return Capabilities.cache(query.getLevel(), blockEntity.getBlockPos(), query.getQuerySide(), capability)
            .flatMap(value -> getBlockDevice(query, value));
    }

    protected abstract Optional<Device> getBlockDevice(final BlockDeviceQuery query, final CapabilityCache<TCapability> value);
}

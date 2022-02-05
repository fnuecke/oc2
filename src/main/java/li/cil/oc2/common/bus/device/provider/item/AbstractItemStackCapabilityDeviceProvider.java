/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;

import java.util.Optional;
import java.util.function.Supplier;

public abstract class AbstractItemStackCapabilityDeviceProvider<TCapability> extends AbstractItemDeviceProvider {
    private final Supplier<Capability<TCapability>> capabilitySupplier;

    ///////////////////////////////////////////////////////////////////

    protected AbstractItemStackCapabilityDeviceProvider(final Supplier<Capability<TCapability>> capabilitySupplier) {
        this.capabilitySupplier = capabilitySupplier;
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final Capability<TCapability> capability = capabilitySupplier.get();
        if (capability == null) throw new IllegalStateException();
        final LazyOptional<TCapability> optional = query.getItemStack().getCapability(capability);
        if (!optional.isPresent()) {
            return Optional.empty();
        }

        final TCapability value = optional.orElseThrow(AssertionError::new);
        final Optional<ItemDevice> device = getItemDevice(query, value);

        return device;
    }

    protected abstract Optional<ItemDevice> getItemDevice(ItemDeviceQuery query, TCapability value);
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.capabilities.CapabilityType;

import java.util.Optional;

public abstract class AbstractItemStackCapabilityDeviceProvider<TCapability> extends AbstractItemDeviceProvider {
    private final CapabilityType<TCapability> capability;

    // ------------------------------------------------------------- //

    protected AbstractItemStackCapabilityDeviceProvider(final CapabilityType<TCapability> capability) {
        this.capability = capability;
    }

    // ------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final TCapability value = Capabilities.get(query.getItemStack(), capability);
        if (value == null) {
            return Optional.empty();
        }

        return getItemDevice(query, value);
    }

    protected abstract Optional<ItemDevice> getItemDevice(ItemDeviceQuery query, TCapability value);
}

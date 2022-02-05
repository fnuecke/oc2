/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.capabilities.Capabilities;

import java.util.Optional;

public class ItemStackCapabilityDeviceProvider extends AbstractItemStackCapabilityDeviceProvider<Device> {
    public ItemStackCapabilityDeviceProvider() {
        super(() -> Capabilities.DEVICE);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query, final Device value) {
        if (value instanceof ItemDevice itemDevice) {
            return Optional.of(itemDevice);
        } else {
            return Optional.empty();
        }
    }
}

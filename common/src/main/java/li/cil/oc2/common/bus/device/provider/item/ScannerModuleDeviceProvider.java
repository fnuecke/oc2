/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.item.ScannerModuleDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class ScannerModuleDeviceProvider extends AbstractItemDeviceProvider {
    public ScannerModuleDeviceProvider() {
        super(Items.SCANNER_MODULE);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerEntity()
            .filter(entity -> Capabilities.get(entity, Capabilities.ROBOT, null) != null)
            .map(entity -> new ScannerModuleDevice(query.getItemStack(), entity));
    }
}

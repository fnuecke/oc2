/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.item.TankOperationsModuleDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class TankOperationsModuleDeviceProvider extends AbstractItemDeviceProvider {
    public TankOperationsModuleDeviceProvider() {
        super(Items.TANK_OPERATIONS_MODULE);
    }

    // --------------------------------------------------------------------- //

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerEntity().flatMap(entity ->
            Optional.ofNullable(Capabilities.get(entity, Capabilities.ROBOT, null)).map(robot ->
                new TankOperationsModuleDevice(query.getItemStack(), entity, robot)));
    }
}

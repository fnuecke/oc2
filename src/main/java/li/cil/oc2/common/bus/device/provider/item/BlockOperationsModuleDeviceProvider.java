/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.Config;
import li.cil.oc2.common.bus.device.item.BlockOperationsModuleDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.item.Items;

import java.util.Optional;

public final class BlockOperationsModuleDeviceProvider extends AbstractItemDeviceProvider {
    public BlockOperationsModuleDeviceProvider() {
        super(Items.BLOCK_OPERATIONS_MODULE);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        return query.getContainerEntity().flatMap(entity ->
            entity.getCapability(Capabilities.ROBOT).map(robot ->
                new BlockOperationsModuleDevice(query.getItemStack(), entity, robot)));
    }

    @Override
    protected int getItemDeviceEnergyConsumption(final ItemDeviceQuery query) {
        return Config.blockOperationsModuleEnergyPerTick;
    }
}

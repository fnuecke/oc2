/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.rpc.ItemHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.items.IItemHandler;

import java.util.Optional;

public final class ItemHandlerItemDeviceProvider extends AbstractItemStackCapabilityDeviceProvider<IItemHandler> {
    public ItemHandlerItemDeviceProvider() {
        super(Capabilities::itemHandler);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query, final IItemHandler value) {
        return Optional.of(new ObjectDevice(new ItemHandlerDevice(value)));
    }
}

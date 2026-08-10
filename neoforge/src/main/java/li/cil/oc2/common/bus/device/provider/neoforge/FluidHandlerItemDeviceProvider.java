/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.neoforge;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.provider.util.AbstractItemDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.neoforge.FluidHandlerDevice;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;

public final class FluidHandlerItemDeviceProvider extends AbstractItemDeviceProvider {
    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query) {
        final IFluidHandlerItem handler = query.getItemStack().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) {
            return Optional.empty();
        }

        return Optional.of(new ObjectDevice(new FluidHandlerDevice(handler)));
    }
}

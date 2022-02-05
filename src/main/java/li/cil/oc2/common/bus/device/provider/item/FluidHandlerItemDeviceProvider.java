/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.item;

import li.cil.oc2.api.bus.device.ItemDevice;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.ItemDeviceQuery;
import li.cil.oc2.common.bus.device.rpc.FluidHandlerDevice;
import li.cil.oc2.common.capabilities.Capabilities;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Optional;

public final class FluidHandlerItemDeviceProvider extends AbstractItemStackCapabilityDeviceProvider<IFluidHandler> {
    public FluidHandlerItemDeviceProvider() {
        super(() -> Capabilities.FLUID_HANDLER);
    }

    ///////////////////////////////////////////////////////////////////

    @Override
    protected Optional<ItemDevice> getItemDevice(final ItemDeviceQuery query, final IFluidHandler value) {
        return Optional.of(new ObjectDevice(new FluidHandlerDevice(value)));
    }
}

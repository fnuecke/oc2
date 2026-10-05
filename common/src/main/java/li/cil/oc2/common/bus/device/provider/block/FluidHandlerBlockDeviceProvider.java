/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.block;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.common.bus.device.FluidHandlerDevice;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import li.cil.oc2.common.capabilities.CachedFluidHandler;
import li.cil.oc2.common.capabilities.Capabilities;

import java.util.Optional;

public final class FluidHandlerBlockDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Optional<Device> getDevice(final BlockDeviceQuery query) {
        return Capabilities.cache(query.getLevel(), query.getQueryPosition(), query.getQuerySide(), Capabilities.FLUID_HANDLER)
            .map(value -> new ObjectDevice(new FluidHandlerDevice(new CachedFluidHandler(value))));
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.provider.neoforge;

import li.cil.oc2.api.bus.device.Device;
import li.cil.oc2.api.bus.device.object.ObjectDevice;
import li.cil.oc2.api.bus.device.provider.BlockDeviceQuery;
import li.cil.oc2.api.util.Invalidatable;
import li.cil.oc2.common.bus.device.provider.util.AbstractBlockDeviceProvider;
import li.cil.oc2.common.bus.device.rpc.neoforge.FluidHandlerDevice;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public final class FluidHandlerBlockDeviceProvider extends AbstractBlockDeviceProvider {
    @Override
    public Invalidatable<Device> getDevice(final BlockDeviceQuery query) {
        if (!(query.getLevel() instanceof final Level level)) {
            return Invalidatable.empty();
        }

        final IFluidHandler handler = level.getCapability(
                Capabilities.FluidHandler.BLOCK, query.getQueryPosition(), query.getQuerySide());
        if (handler == null) {
            return Invalidatable.empty();
        }

        return Invalidatable.of(new ObjectDevice(new FluidHandlerDevice(handler)));
    }
}

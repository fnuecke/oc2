/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import java.util.Collection;
import java.util.Collections;

public final class FluidHandlerDevice extends IdentityProxy<IFluidHandler> implements NamedDevice {
    public FluidHandlerDevice(final IFluidHandler identity) {
        super(identity);
    }

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singletonList("fluid_handler");
    }

    @Callback
    public int getFluidTanks() {
        return identity.getTanks();
    }

    @Callback
    public FluidStack getFluidInTank(final int tank) {
        return identity.getFluidInTank(tank);
    }

    @Callback
    public int getFluidTankCapacity(final int tank) {
        return identity.getTankCapacity(tank);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOName;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.NamedDevice;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.common.bus.device.util.FluidHandlerProtocol;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;

@IOName("FLUIDS")
public final class FluidHandlerDevice extends IdentityProxy<FluidHandler> implements NamedDevice {
    private static final int GET_TANK_COUNT_CODE = 1;
    private static final int GET_TANKS_CODE = 2;
    private static final int GET_TANK_CAPACITY_CODE = 3;
    private static final int GET_FLUID_NAME_CODE = 4;
    private static final int GET_FLUID_ID_CODE = 5;

    // --------------------------------------------------------------------- //

    public FluidHandlerDevice(final FluidHandler identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Override
    public Collection<String> getDeviceTypeNames() {
        return Collections.singleton("fluid_handler");
    }

    @Callback
    public int getFluidTankCount() {
        return identity.getTanks();
    }

    @Callback
    public FluidStack getFluidInTank(@Parameter("tank") final int tank) {
        return identity.getFluidInTank(FluidHandlerProtocol.requireValidTank(identity, tank));
    }

    @Callback
    public int getFluidTankCapacity(@Parameter("tank") final int tank) {
        return identity.getTankCapacity(FluidHandlerProtocol.requireValidTank(identity, tank));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(GET_TANK_COUNT_CODE)
    public void getFluidTankCountIO(final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCount(identity, results);
    }

    @IOCallback(GET_TANKS_CODE)
    public void getFluidTanksIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(identity, arguments, results);
    }

    @IOCallback(GET_TANK_CAPACITY_CODE)
    public void getFluidTankCapacityIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(identity, arguments, results);
    }

    @IOCallback(value = GET_FLUID_NAME_CODE, synchronize = false)
    public void getFluidNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidName(arguments, results);
    }

    @IOCallback(value = GET_FLUID_ID_CODE, synchronize = false)
    public void getFluidIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidId(arguments, results);
    }
}

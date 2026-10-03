/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.*;
import li.cil.oc2.common.bus.device.util.FluidHandlerDeviceUtils;
import li.cil.oc2.common.bus.device.util.FluidHandlerProtocol;
import li.cil.oc2.common.bus.device.util.IdentityProxy;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;

import java.io.IOException;

@RPCDeviceDescription(typeNames = {"fluid_handler"}, description = """
    Provided by any tank connected through a [bus interface](../block/bus_interface.md), including cauldrons and the tanks of other mods. To move fluids between tanks, use a [transposer](../block/transposer.md). Amounts are in millibuckets, a bucket being 1000.

    With several tanks connected, `find` may return any of them. To grab a specific one, give the bus interface in front of the one you want a name with a [wrench](../item/wrench.md), and find it by that name instead.""")
@IODeviceDescription(name = "FLUIDS", description = """
    Each container is listed separately. When several are connected, pass a count in `B` to `OCFIND` to pick one, or check what `DEVS` lists.

    Fluid ids are two bytes, low byte first; [`SYSTEM`](system.md) provides name lookup. An empty tank reads as fluid 0. Amounts are four bytes, low byte first.""")
public final class FluidHandlerDevice extends IdentityProxy<FluidHandler> {
    private static final int GET_TANK_COUNT_CODE = 1;
    private static final int GET_TANKS_CODE = 2;
    private static final int GET_TANK_CAPACITY_CODE = 3;

    private static final String TANK = "the number of the tank to look at.";

    // --------------------------------------------------------------------- //

    public FluidHandlerDevice(final FluidHandler identity) {
        super(identity);
    }

    // --------------------------------------------------------------------- //

    @Callback(description = "Gets how many tanks the container has.",
        returnValueDescription = "the number of tanks.")
    public int getFluidTankCount() {
        return identity.getTanks();
    }

    @Callback(description = "Gets what is in the specified tank.",
        returnValueDescription = "a table with the fluid `id`, such as `minecraft:water`, and the `amount` in millibuckets. Returns nothing for an empty tank.")
    public FluidStack getFluidInTank(@Parameter(value = "tank", description = TANK) final int tank) {
        return identity.getFluidInTank(FluidHandlerDeviceUtils.requireValidTank(identity, tank));
    }

    @Callback(description = "Gets how much the specified tank can hold.",
        returnValueDescription = "the capacity in millibuckets.")
    public int getFluidTankCapacity(@Parameter(value = "tank", description = TANK) final int tank) {
        return identity.getTankCapacity(FluidHandlerDeviceUtils.requireValidTank(identity, tank));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_TANK_COUNT_CODE,
        description = "Reads how many tanks the container has.",
        resultsDescription = "one byte, the tank count, at most 255.")
    public void getFluidTankCount(final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCount(identity, results);
    }

    @IOCallback(value = GET_TANKS_CODE,
        description = """
            Reads a run of tanks in one call.
            Ask for 1 to 42 tanks; more than that fails with `OCEARG`, since the reply would not fit. Reading stops at the end of the container, so asking for 42 tanks starting at 0 gives you as many as there are.""",
        argumentsDescription = "two bytes, the tank to start at and how many tanks to read.",
        resultsDescription = "six bytes per tank: the fluid as two bytes and the amount as four bytes.")
    public void getFluidTanks(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(identity, arguments, results);
    }

    @IOCallback(value = GET_TANK_CAPACITY_CODE,
        description = "Reads how much the tank can hold.",
        argumentsDescription = "one byte, the tank.",
        resultsDescription = "four bytes, the capacity.")
    public void getFluidTankCapacity(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(identity, arguments, results);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.rpc;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IODeviceDescription;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.Callback;
import li.cil.oc2.api.bus.device.object.Parameter;
import li.cil.oc2.api.bus.device.object.RPCDeviceDescription;
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

    Fluids are identified by ids to save bus bandwidth. An id is stable within a world, but not across changes to the installed mods. An empty tank reads as fluid 0. Fluid numbers are two bytes, low byte first. Amounts are four bytes, low byte first.

    A tank the container does not have, or a name no fluid goes by, fails with `OCEARG`.""")
public final class FluidHandlerDevice extends IdentityProxy<FluidHandler> {
    private static final int GET_TANK_COUNT_CODE = 1;
    private static final int GET_TANKS_CODE = 2;
    private static final int GET_TANK_CAPACITY_CODE = 3;
    private static final int GET_FLUID_NAME_CODE = 4;
    private static final int GET_FLUID_ID_CODE = 5;

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
        return identity.getFluidInTank(FluidHandlerProtocol.requireValidTank(identity, tank));
    }

    @Callback(description = "Gets how much the specified tank can hold.",
        returnValueDescription = "the capacity in millibuckets.")
    public int getFluidTankCapacity(@Parameter(value = "tank", description = TANK) final int tank) {
        return identity.getTankCapacity(FluidHandlerProtocol.requireValidTank(identity, tank));
    }

    // --------------------------------------------------------------------- //

    @IOCallback(value = GET_TANK_COUNT_CODE, name = "getTankCount",
        description = "Reads how many tanks the container has.",
        resultsDescription = "one byte, the tank count, at most 255.")
    public void getFluidTankCountIO(final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCount(identity, results);
    }

    @IOCallback(value = GET_TANKS_CODE, name = "getTanks",
        description = """
            Reads a run of tanks in one call.
            Ask for 1 to 42 tanks; more than that fails with `OCEARG`, since the reply would not fit. Reading stops at the end of the container, so asking for 42 tanks starting at 0 gives you as many as there are.""",
        argumentsDescription = "two bytes, the tank to start at and how many tanks to read.",
        resultsDescription = "six bytes per tank: the fluid as two bytes and the amount as four bytes.")
    public void getFluidTanksIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTanks(identity, arguments, results);
    }

    @IOCallback(value = GET_TANK_CAPACITY_CODE, name = "getTankCapacity",
        description = "Reads how much the tank can hold.",
        argumentsDescription = "one byte, the tank.",
        resultsDescription = "four bytes, the capacity.")
    public void getFluidTankCapacityIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeTankCapacity(identity, arguments, results);
    }

    @IOCallback(value = GET_FLUID_NAME_CODE, synchronize = false, name = "getFluidName",
        description = "Reads the name of a fluid.",
        argumentsDescription = "two bytes, the fluid id.",
        resultsDescription = "the name, such as `minecraft:water`. Read while `OCDAV` is set to get all of it.")
    public void getFluidNameIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidName(arguments, results);
    }

    @IOCallback(value = GET_FLUID_ID_CODE, synchronize = false, name = "getFluidId",
        description = "Looks a fluid up by name.",
        argumentsDescription = "the name, with or without a zero byte at the end. Leave off the `minecraft:` and it is assumed.",
        resultsDescription = "two bytes, the fluid id.")
    public void getFluidIdIO(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        FluidHandlerProtocol.writeFluidId(arguments, results);
    }
}

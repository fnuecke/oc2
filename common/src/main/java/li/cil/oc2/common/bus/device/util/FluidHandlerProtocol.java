/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.api.bus.device.object.IOCallback;
import li.cil.oc2.common.bus.device.SystemDevice;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;

import java.io.IOException;

public final class FluidHandlerProtocol {
    private static final int TANK_RECORD_SIZE = 6;
    private static final int MAX_TANK_RECORDS = IOCallback.MAX_DATA_SIZE / TANK_RECORD_SIZE;

    // --------------------------------------------------------------------- //

    public static void writeTankCount(final FluidHandler handler, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(handler.getTanks(), 0xFF));
    }

    public static void writeTanks(final FluidHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int first = FluidHandlerDeviceUtils.requireValidTank(handler, arguments.readU8());
        final int requested = arguments.readU8();
        if (requested == 0 || requested > MAX_TANK_RECORDS) {
            throw new IllegalArgumentException("tank count out of range: " + requested
                + " (expected 1 to " + MAX_TANK_RECORDS + ")");
        }

        final int count = Math.min(requested, handler.getTanks() - first);
        for (int tank = first; tank < first + count; tank++) {
            writeTankAt(handler, tank, results);
        }
    }

    public static void writeTankCapacity(final FluidHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        results.writeU32(handler.getTankCapacity(FluidHandlerDeviceUtils.requireValidTank(handler, arguments.readU8())));
    }

    public static void writeTankAt(final FluidHandler handler, final int tank, final IOOutputStream results) throws IOException {
        final FluidStack stack = handler.getFluidInTank(tank);
        results.writeU16(SystemDevice.toId(BuiltInRegistries.FLUID, stack.fluid()));
        results.writeU32(stack.amount());
    }

    // --------------------------------------------------------------------- //

    private FluidHandlerProtocol() {
    }
}

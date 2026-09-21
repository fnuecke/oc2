/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.bus.device.util;

import li.cil.oc2.api.bus.device.io.IOCallback;
import li.cil.oc2.api.bus.device.io.IOInputStream;
import li.cil.oc2.api.bus.device.io.IOOutputStream;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;

import java.io.IOException;

public final class FluidHandlerProtocol {
    private static final int TANK_RECORD_SIZE = 6;
    private static final int MAX_TANK_RECORDS = IOCallback.MAX_DATA_SIZE / TANK_RECORD_SIZE;

    // --------------------------------------------------------------------- //

    public static int requireValidTank(final FluidHandler handler, final int tank) {
        if (tank < 0 || tank >= handler.getTanks()) {
            throw new IllegalArgumentException("tank out of range: " + tank
                + " (expected 0 to " + (handler.getTanks() - 1) + ")");
        }
        return tank;
    }

    public static void writeTankCount(final FluidHandler handler, final IOOutputStream results) throws IOException {
        results.writeU8(Math.min(handler.getTanks(), 0xFF));
    }

    public static void writeTanks(final FluidHandler handler, final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int first = requireValidTank(handler, arguments.readU8());
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
        results.writeU32(handler.getTankCapacity(requireValidTank(handler, arguments.readU8())));
    }

    public static void writeFluidName(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final int id = arguments.readU16();
        final ResourceLocation key = BuiltInRegistries.FLUID.getHolder(id)
            .orElseThrow(() -> new IllegalArgumentException("no fluid with id: " + id))
            .key().location();
        results.writeString(key.toString());
    }

    public static void writeFluidId(final IOInputStream arguments, final IOOutputStream results) throws IOException {
        final String name = arguments.readString();
        final ResourceLocation key = ResourceLocation.tryParse(name);
        final Fluid fluid = key != null ? BuiltInRegistries.FLUID.getOptional(key).orElse(null) : null;
        if (fluid == null) {
            throw new IllegalArgumentException("no such fluid: " + name);
        }

        results.writeU16(toFluidId(fluid));
    }

    // --------------------------------------------------------------------- //

    private static void writeTankAt(final FluidHandler handler, final int tank, final IOOutputStream results) throws IOException {
        final FluidStack stack = handler.getFluidInTank(tank);
        results.writeU16(toFluidId(stack.fluid()));
        results.writeU32(stack.amount());
    }

    private static int toFluidId(final Fluid fluid) throws IOException {
        final int id = BuiltInRegistries.FLUID.getId(fluid);
        if (id > 0xFFFF) {
            throw new IOException("fluid id does not fit the guest protocol: " + id);
        }
        return id;
    }

    private FluidHandlerProtocol() {
    }
}

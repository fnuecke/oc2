/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;

import javax.annotation.Nullable;

public final class ContainerFluidHandler implements FluidHandler {
    private final ContainerItemAccess<FluidHandler> access;

    // --------------------------------------------------------------------- //

    private ContainerFluidHandler(final ContainerItemAccess<FluidHandler> access) {
        this.access = access;
    }

    @Nullable
    public static FluidHandler of(final ItemHandler inventory, final int slot) {
        final ContainerItemAccess<FluidHandler> access = new ContainerItemAccess<>(inventory, slot, Capabilities.FLUID_HANDLER);
        return access.isPresent() ? new ContainerFluidHandler(access) : null;
    }

    // --------------------------------------------------------------------- //

    @Override
    public int getTanks() {
        return access.read(FluidHandler::getTanks, 0);
    }

    @Override
    public FluidStack getFluidInTank(final int tank) {
        return access.read(handler -> handler.getFluidInTank(tank), FluidStack.EMPTY);
    }

    @Override
    public int getTankCapacity(final int tank) {
        return access.read(handler -> handler.getTankCapacity(tank), 0);
    }

    @Override
    public int fill(final FluidStack stack, final boolean simulate) {
        return simulate
            ? access.read(handler -> handler.fill(stack, true), 0)
            : access.modify(handler -> handler.fill(stack, false), 0);
    }

    @Override
    public FluidStack drain(final FluidStack stack, final boolean simulate) {
        return simulate
            ? access.read(handler -> handler.drain(stack, true), FluidStack.EMPTY)
            : access.modify(handler -> handler.drain(stack, false), FluidStack.EMPTY);
    }

    @Override
    public FluidStack drain(final int amount, final boolean simulate) {
        return simulate
            ? access.read(handler -> handler.drain(amount, true), FluidStack.EMPTY)
            : access.modify(handler -> handler.drain(amount, false), FluidStack.EMPTY);
    }
}

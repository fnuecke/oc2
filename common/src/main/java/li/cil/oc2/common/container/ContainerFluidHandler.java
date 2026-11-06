/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.container;

import li.cil.oc2.common.capabilities.Capabilities;
import li.cil.oc2.common.fluid.FluidHandler;
import li.cil.oc2.common.fluid.FluidStack;
import li.cil.oc2.common.inventory.ItemHandler;
import li.cil.oc2.common.transaction.TransactionContext;

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
    public int fill(final FluidStack stack, final TransactionContext transaction) {
        return access.modify((handler, nested) -> handler.fill(stack, nested), 0, transaction);
    }

    @Override
    public FluidStack drain(final FluidStack stack, final TransactionContext transaction) {
        return access.modify((handler, nested) -> handler.drain(stack, nested), FluidStack.EMPTY, transaction);
    }

    @Override
    public FluidStack drain(final int amount, final TransactionContext transaction) {
        return access.modify((handler, nested) -> handler.drain(amount, nested), FluidStack.EMPTY, transaction);
    }
}

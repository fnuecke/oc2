/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import li.cil.oc2.common.transaction.TransactionContext;

public interface FluidHandler {
    int BUCKET = 1000;

    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    int fill(FluidStack stack, TransactionContext transaction);

    FluidStack drain(FluidStack stack, TransactionContext transaction);

    FluidStack drain(int amount, TransactionContext transaction);
}

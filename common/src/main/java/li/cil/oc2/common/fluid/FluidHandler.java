/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

public interface FluidHandler {
    int BUCKET = 1000;

    int getTanks();

    FluidStack getFluidInTank(int tank);

    int getTankCapacity(int tank);

    int fill(FluidStack stack, boolean simulate);

    FluidStack drain(FluidStack stack, boolean simulate);

    FluidStack drain(int amount, boolean simulate);
}

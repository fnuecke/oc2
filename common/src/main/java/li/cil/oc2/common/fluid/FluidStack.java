/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public record FluidStack(Fluid fluid, DataComponentPatch components, int amount) {
    public static final FluidStack EMPTY = new FluidStack(Fluids.EMPTY, DataComponentPatch.EMPTY, 0);

    public FluidStack(final Fluid fluid, final int amount) {
        this(fluid, DataComponentPatch.EMPTY, amount);
    }

    public boolean isEmpty() {
        return fluid == Fluids.EMPTY || amount <= 0;
    }

    public boolean isSameFluid(final FluidStack other) {
        return fluid == other.fluid && components.equals(other.components);
    }

    public FluidStack withAmount(final int amount) {
        return new FluidStack(fluid, components, amount);
    }
}

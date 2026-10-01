/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import java.util.List;
import java.util.function.BiFunction;

public final class FluidHandlerUtils {
    public static FluidStack insertFluidStack(final FluidHandler handler, final FluidStack stack, final boolean simulate) {
        if (stack.isEmpty()) {
            return FluidStack.EMPTY;
        }

        final int filled = handler.fill(stack, simulate);
        return filled >= stack.amount() ? FluidStack.EMPTY : stack.withAmount(stack.amount() - filled);
    }

    public static int transfer(final FluidHandler source, final int amount, final BiFunction<FluidStack, Boolean, FluidStack> insert) {
        if (amount <= 0) {
            return 0;
        }

        final FluidStack available = source.drain(amount, true);
        if (available.isEmpty()) {
            return 0;
        }

        final int accepted = available.amount() - insert.apply(available, true).amount();
        if (accepted <= 0) {
            return 0;
        }

        final FluidStack extracted = source.drain(available.withAmount(accepted), false);
        if (extracted.isEmpty()) {
            return 0;
        }

        final FluidStack remaining = insert.apply(extracted, false);
        if (!remaining.isEmpty()) {
            source.fill(remaining, false);
        }

        return extracted.amount() - remaining.amount();
    }

    public static int transferFirst(final List<FluidHandler> sources, final List<FluidHandler> targets, final int amount) {
        for (final FluidHandler source : sources) {
            for (final FluidHandler target : targets) {
                final int moved = transfer(source, amount, (stack, simulate) -> insertFluidStack(target, stack, simulate));
                if (moved > 0) {
                    return moved;
                }
            }
        }

        return 0;
    }

    // --------------------------------------------------------------------- //

    private FluidHandlerUtils() {
    }
}

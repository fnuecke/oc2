/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.fluid;

import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;

import java.util.List;

public final class FluidHandlerUtils {
    public static int transfer(final FluidHandler source, final FluidHandler target, final int amount, final TransactionContext transaction) {
        if (amount <= 0) {
            return 0;
        }

        final FluidStack accepted;
        try (Transaction nested = transaction.openNested()) {
            final FluidStack available = source.drain(amount, nested);
            if (available.isEmpty()) {
                return 0;
            }

            accepted = available.withAmount(target.fill(available, nested));
        }

        if (accepted.isEmpty()) {
            return 0;
        }

        try (Transaction nested = transaction.openNested()) {
            final FluidStack drained = source.drain(accepted, nested);
            if (drained.isEmpty() || target.fill(drained, nested) != drained.amount()) {
                return 0;
            }

            nested.commit();
            return drained.amount();
        }
    }

    public static int transferFirst(final List<FluidHandler> sources, final List<FluidHandler> targets, final int amount,
                                    final TransactionContext transaction) {
        for (final FluidHandler source : sources) {
            for (final FluidHandler target : targets) {
                final int moved = transfer(source, target, amount, transaction);
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

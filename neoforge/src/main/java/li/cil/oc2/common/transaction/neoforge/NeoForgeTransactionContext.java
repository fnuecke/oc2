/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction.neoforge;

import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;

public record NeoForgeTransactionContext(
    net.neoforged.neoforge.transfer.transaction.TransactionContext context) implements TransactionContext {
    public static net.neoforged.neoforge.transfer.transaction.TransactionContext unwrap(final TransactionContext context) {
        if (context instanceof TransactionImpl(var inner)) {
            return inner;
        }
        return ((NeoForgeTransactionContext) context).context();
    }

    // --------------------------------------------------------------------- //

    @Override
    public Transaction openNested() {
        return new TransactionImpl(net.neoforged.neoforge.transfer.transaction.Transaction.open(context));
    }
}

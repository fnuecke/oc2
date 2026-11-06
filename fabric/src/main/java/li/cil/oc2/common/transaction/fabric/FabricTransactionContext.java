/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction.fabric;

import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;

public record FabricTransactionContext(
    net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext context) implements TransactionContext {
    public static net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext unwrap(final TransactionContext context) {
        if (context instanceof TransactionImpl(var inner)) {
            return inner;
        }
        return ((FabricTransactionContext) context).context();
    }

    // --------------------------------------------------------------------- //

    @Override
    public Transaction openNested() {
        return new TransactionImpl(context.openNested());
    }
}

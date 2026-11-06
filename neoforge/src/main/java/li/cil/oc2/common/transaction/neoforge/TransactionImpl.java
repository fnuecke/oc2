/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction.neoforge;

import li.cil.oc2.common.transaction.Transaction;

public record TransactionImpl(net.neoforged.neoforge.transfer.transaction.Transaction transaction) implements Transaction {
    public static Transaction openRoot() {
        return new TransactionImpl(net.neoforged.neoforge.transfer.transaction.Transaction.openRoot());
    }

    // --------------------------------------------------------------------- //

    @Override
    public Transaction openNested() {
        return new TransactionImpl(net.neoforged.neoforge.transfer.transaction.Transaction.open(transaction));
    }

    @Override
    public void commit() {
        transaction.commit();
    }

    @Override
    public void close() {
        transaction.close();
    }
}

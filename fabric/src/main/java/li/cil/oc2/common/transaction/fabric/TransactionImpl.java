/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction.fabric;

import li.cil.oc2.common.transaction.Transaction;

public record TransactionImpl(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction transaction) implements Transaction {
    public static Transaction openRoot() {
        return new TransactionImpl(net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter());
    }

    // --------------------------------------------------------------------- //

    @Override
    public Transaction openNested() {
        return new TransactionImpl(transaction.openNested());
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

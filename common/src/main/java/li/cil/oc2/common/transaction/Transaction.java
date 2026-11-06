/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.function.Function;

public interface Transaction extends TransactionContext, AutoCloseable {
    @ExpectPlatform
    static Transaction openRoot() {
        throw new AssertionError();
    }

    static <T> T run(final Function<TransactionContext, T> operation) {
        try (Transaction transaction = openRoot()) {
            final T result = operation.apply(transaction);
            transaction.commit();
            return result;
        }
    }

    void commit();

    @Override
    void close();
}

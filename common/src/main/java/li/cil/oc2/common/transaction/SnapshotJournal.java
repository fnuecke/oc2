/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface SnapshotJournal {
    @ExpectPlatform
    static <T> SnapshotJournal create(final Supplier<T> capture, final Consumer<T> reset, final Runnable onRootCommit) {
        throw new AssertionError();
    }

    void updateSnapshots(TransactionContext transaction);
}

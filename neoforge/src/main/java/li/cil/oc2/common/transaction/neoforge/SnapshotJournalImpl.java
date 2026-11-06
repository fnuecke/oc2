/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction.neoforge;

import li.cil.oc2.common.transaction.SnapshotJournal;
import li.cil.oc2.common.transaction.TransactionContext;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SnapshotJournalImpl<T> extends net.neoforged.neoforge.transfer.transaction.SnapshotJournal<T> implements SnapshotJournal {
    private final Supplier<T> capture;
    private final Consumer<T> reset;
    private final Runnable onRootCommit;

    // --------------------------------------------------------------------- //

    private SnapshotJournalImpl(final Supplier<T> capture, final Consumer<T> reset, final Runnable onRootCommit) {
        this.capture = capture;
        this.reset = reset;
        this.onRootCommit = onRootCommit;
    }

    public static <T> SnapshotJournal create(final Supplier<T> capture, final Consumer<T> reset, final Runnable onRootCommit) {
        return new SnapshotJournalImpl<>(capture, reset, onRootCommit);
    }

    // --------------------------------------------------------------------- //

    @Override
    public void updateSnapshots(final TransactionContext transaction) {
        updateSnapshots(NeoForgeTransactionContext.unwrap(transaction));
    }

    // --------------------------------------------------------------------- //

    @Override
    protected T createSnapshot() {
        return capture.get();
    }

    @Override
    protected void revertToSnapshot(final T snapshot) {
        reset.accept(snapshot);
    }

    @Override
    protected void onRootCommit(final T originalState) {
        onRootCommit.run();
    }
}

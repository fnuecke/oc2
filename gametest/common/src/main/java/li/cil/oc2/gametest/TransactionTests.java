/* SPDX-License-Identifier: MIT */

package li.cil.oc2.gametest;

import li.cil.oc2.common.transaction.SnapshotJournal;
import li.cil.oc2.common.transaction.Transaction;
import li.cil.oc2.common.transaction.TransactionContext;
import net.minecraft.gametest.framework.GameTestHelper;

import static li.cil.oc2.gametest.util.TestSupport.assertEquals;

public final class TransactionTests {
    public static void closingRevertsNestedChanges(final GameTestHelper helper) {
        final Value value = new Value();

        try (Transaction root = Transaction.openRoot()) {
            value.set(root, 1);
            try (Transaction nested = root.openNested()) {
                value.set(nested, 2);
            }
            assertEquals(helper, "value after closing a nested transaction", 1, value.value);

            try (Transaction nested = root.openNested()) {
                value.set(nested, 3);
                nested.commit();
            }
            assertEquals(helper, "value after committing a nested transaction", 3, value.value);
        }

        assertEquals(helper, "value after closing the root", 0, value.value);
        assertEquals(helper, "commit hook calls after closing the root", 0, value.commits);
        helper.succeed();
    }

    public static void commitHookRunsOncePerRootCommit(final GameTestHelper helper) {
        final Value value = new Value();

        try (Transaction root = Transaction.openRoot()) {
            value.set(root, 1);
            for (int i = 2; i <= 3; i++) {
                try (Transaction nested = root.openNested()) {
                    value.set(nested, i);
                    nested.commit();
                }
            }
            assertEquals(helper, "commit hook calls before the root commit", 0, value.commits);
            root.commit();
        }
        assertEquals(helper, "value after the root commit", 3, value.value);
        assertEquals(helper, "commit hook calls after the root commit", 1, value.commits);

        try (Transaction root = Transaction.openRoot()) {
            root.commit();
        }
        assertEquals(helper, "commit hook calls after a root commit without changes", 1, value.commits);

        try (Transaction root = Transaction.openRoot()) {
            value.set(root, 4);
            root.commit();
        }
        assertEquals(helper, "commit hook calls after a second root commit", 2, value.commits);

        helper.succeed();
    }

    // --------------------------------------------------------------------- //

    private static final class Value {
        private int value;
        private int commits;
        private final SnapshotJournal journal = SnapshotJournal.create(() -> value, snapshot -> value = snapshot, () -> commits++);

        private void set(final TransactionContext transaction, final int value) {
            journal.updateSnapshots(transaction);
            this.value = value;
        }
    }

    // --------------------------------------------------------------------- //

    private TransactionTests() {
    }
}

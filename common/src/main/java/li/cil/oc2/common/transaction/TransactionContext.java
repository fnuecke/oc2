/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.transaction;

public interface TransactionContext {
    Transaction openNested();
}

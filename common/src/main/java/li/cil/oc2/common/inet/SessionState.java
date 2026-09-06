/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet;

public enum SessionState {
    NEW,
    ESTABLISHED,
    FINISH,
    REJECT,
    EXPIRED;

    public boolean isClosed() {
        return this == FINISH || this == REJECT || this == EXPIRED;
    }
}

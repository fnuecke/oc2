/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

public enum Align {
    START,
    CENTER,
    END;

    public int offset(final int free) {
        return switch (this) {
            case START -> 0;
            case CENTER -> free / 2;
            case END -> free;
        };
    }
}

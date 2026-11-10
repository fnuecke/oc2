/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

public enum Axis {
    HORIZONTAL,
    VERTICAL;

    public Axis other() {
        return this == HORIZONTAL ? VERTICAL : HORIZONTAL;
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

public record Insets(int left, int top, int right, int bottom) {
    public static final Insets NONE = new Insets(0, 0, 0, 0);

    public static Insets of(final int all) {
        return new Insets(all, all, all, all);
    }

    public int start(final Axis axis) {
        return axis == Axis.HORIZONTAL ? left : top;
    }

    public int total(final Axis axis) {
        return axis == Axis.HORIZONTAL ? left + right : top + bottom;
    }
}

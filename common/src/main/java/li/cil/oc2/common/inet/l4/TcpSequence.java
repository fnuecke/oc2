/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.inet.l4;

public final class TcpSequence {
    public static boolean lt(final int a, final int b) {
        return a - b < 0;
    }

    public static boolean leq(final int a, final int b) {
        return a - b <= 0;
    }

    public static boolean gt(final int a, final int b) {
        return a - b > 0;
    }

    public static boolean geq(final int a, final int b) {
        return a - b >= 0;
    }

    public static boolean between(final int value, final int low, final int high) {
        return geq(value, low) && lt(value, high);
    }

    // --------------------------------------------------------------------- //

    private TcpSequence() {
    }
}

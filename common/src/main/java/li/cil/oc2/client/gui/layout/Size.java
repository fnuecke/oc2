/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

public sealed interface Size {
    Size FIT = new Fit();
    Size GROW = new Grow();

    static Size fixed(final int value) {
        return new Fixed(value);
    }

    // --------------------------------------------------------------------- //

    record Fixed(int value) implements Size {
    }

    record Fit() implements Size {
    }

    record Grow() implements Size {
    }
}

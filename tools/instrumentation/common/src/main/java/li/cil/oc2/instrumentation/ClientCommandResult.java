/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import javax.annotation.Nullable;

public final class ClientCommandResult {
    @Nullable
    private static String value;

    // --------------------------------------------------------------------- //

    public static void set(final String result) {
        value = result;
    }

    public static void clear() {
        value = null;
    }

    public static String take(final String fallback) {
        final String result = value;
        value = null;
        return result != null ? result : fallback;
    }

    // --------------------------------------------------------------------- //

    private ClientCommandResult() {
    }
}

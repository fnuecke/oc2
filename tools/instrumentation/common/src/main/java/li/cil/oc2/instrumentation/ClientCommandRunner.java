/* SPDX-License-Identifier: MIT */

package li.cil.oc2.instrumentation;

import dev.architectury.injectables.annotations.ExpectPlatform;

public final class ClientCommandRunner {
    @ExpectPlatform
    public static String run(final String command) {
        throw new AssertionError();
    }

    private ClientCommandRunner() {
    }
}

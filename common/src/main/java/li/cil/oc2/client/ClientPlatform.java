/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client;

import dev.architectury.injectables.annotations.ExpectPlatform;

public final class ClientPlatform {
    @ExpectPlatform
    public static void setHotbarVisible(final boolean value) {
        throw new AssertionError();
    }

    private ClientPlatform() {
    }
}

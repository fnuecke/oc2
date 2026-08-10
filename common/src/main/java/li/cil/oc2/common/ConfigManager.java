/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.util.function.Supplier;

public final class ConfigManager {
    @ExpectPlatform
    public static <T> void add(final Supplier<T> factory) {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static void initialize() {
        throw new AssertionError();
    }

    private ConfigManager() {
    }
}

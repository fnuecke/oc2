/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Component;
import li.cil.oc2.api.API;

public final class TranslationUtils {
    public static String key(final String pattern) {
        return pattern.replaceAll("\\{mod}", API.MOD_ID);
    }

    public static MutableComponent text(final String pattern) {
        return Component.translatable(key(pattern));
    }

    private TranslationUtils() {
    }
}

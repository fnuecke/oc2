/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

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

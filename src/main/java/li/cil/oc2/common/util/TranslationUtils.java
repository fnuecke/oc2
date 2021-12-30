package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.network.chat.TranslatableComponent;

public final class TranslationUtils {
    public static String key(final String pattern) {
        return pattern.replaceAll("\\{mod}", API.MOD_ID);
    }

    public static TranslatableComponent text(final String pattern) {
        return new TranslatableComponent(key(pattern));
    }

    private TranslationUtils() {
    }
}

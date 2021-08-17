package li.cil.oc2.common.util;

import li.cil.oc2.api.API;
import net.minecraft.util.text.TranslationTextComponent;

public final class TranslationUtils {
    public static String key(final String pattern) {
        return pattern.replaceAll("\\{mod}", API.MOD_ID);
    }

    public static TranslationTextComponent text(final String pattern) {
        return new TranslationTextComponent(key(pattern));
    }

    private TranslationUtils() {
    }
}

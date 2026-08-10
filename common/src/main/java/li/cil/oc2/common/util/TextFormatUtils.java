/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.util;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;

public final class TextFormatUtils {
    private static final int SIZE_STEP = 1024;
    private static final String[] SIZE_FORMAT = {"%dB", "%dKB", "%dMB", "%dGB", "%dTB"};

    public static String formatSize(long size) {
        int index = 0;
        while (size > SIZE_STEP && index < SIZE_FORMAT.length) {
            size /= SIZE_STEP;
            index++;
        }
        return String.format(SIZE_FORMAT[index], size);
    }

    public static MutableComponent withFormat(final String value, final ChatFormatting formatting) {
        return withFormat(Component.literal(value), formatting);
    }

    public static MutableComponent withFormat(final MutableComponent text, final ChatFormatting formatting) {
        return text.withStyle(s -> s.withColor(TextColor.fromLegacyFormat(formatting)));
    }

    public static Component withFormat(final Component text, final ChatFormatting formatting) {
        return Component.literal("").withStyle(formatting).append(text);
    }
}

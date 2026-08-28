/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.StringSplitter;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

import java.util.List;

public final class TooltipRenderer {
    public static void drawTooltip(final GuiGraphics graphics, final List<? extends FormattedText> tooltip, final int x, final int y) {
        drawTooltip(graphics, tooltip, x, y, 200);
    }

    public static void drawTooltip(final GuiGraphics graphics, final List<? extends FormattedText> tooltip, final int x, final int y, final int widthHint) {
        final Minecraft minecraft = Minecraft.getInstance();
        final Screen screen = minecraft.screen;
        if (screen == null) {
            return;
        }

        final int availableWidth = Math.max(x, screen.width - x);
        final int targetWidth = Math.min(availableWidth, widthHint);
        final Font font = minecraft.font;

        final StringSplitter splitter = font.getSplitter();
        final boolean needsWrapping = tooltip.stream().anyMatch(line -> font.width(line) > targetWidth);
        final List<? extends FormattedText> lines = needsWrapping
            ? tooltip.stream().flatMap(line -> splitter.splitLines(line, targetWidth, Style.EMPTY).stream()).toList()
            : tooltip;
        graphics.renderTooltip(font, lines.stream().map(Language.getInstance()::getVisualOrder).toList(), x, y);
    }

    // --------------------------------------------------------------------- //

    private TooltipRenderer() {
    }
}

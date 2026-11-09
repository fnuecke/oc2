/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.ToIntFunction;

public class LabelNode extends Node {
    private static final int LABEL_INSET = 1;
    private static final String ELLIPSIS = "...";

    // --------------------------------------------------------------------- //

    private final Component text;
    private final boolean allowTruncating;
    private ToIntFunction<Theme> color = Theme::titleColor;
    @Nullable
    private Component cache;
    private int cacheKey = -1;
    private boolean isTruncated;

    // --------------------------------------------------------------------- //

    public static LabelNode truncating(final Component text) {
        return new LabelNode(text, true);
    }

    // --------------------------------------------------------------------- //

    public LabelNode(final Component text) {
        this(text, false);
    }

    private LabelNode(final Component text, final boolean allowTruncating) {
        this.text = text;
        this.allowTruncating = allowTruncating;
        if (allowTruncating) {
            width(Size.GROW);
        }
    }

    // --------------------------------------------------------------------- //

    public LabelNode color(final ToIntFunction<Theme> color) {
        this.color = color;
        return this;
    }

    // --------------------------------------------------------------------- //

    @Override
    protected int contentWidth() {
        return allowTruncating ? 0 : LABEL_INSET + font().width(text);
    }

    @Override
    protected int contentHeight() {
        return font().lineHeight;
    }

    @Override
    protected void drawForeground(final UiRenderer renderer, final int mouseX, final int mouseY) {
        renderer.text(getOrBuildComponent(), x + LABEL_INSET, y, color.applyAsInt(renderer.theme()), false);
    }

    @Nullable
    @Override
    protected List<Component> getTooltip() {
        final List<Component> tooltip = super.getTooltip();
        if (tooltip == null && isTruncated()) {
            return List.of(text);
        }
        return tooltip;
    }

    // --------------------------------------------------------------------- //

    private Component getOrBuildComponent() {
        if (cacheKey != w || cache == null) {
            final Font font = font();
            final int available = w - LABEL_INSET;
            cacheKey = w;
            isTruncated = font.width(text) > available;
            cache = isTruncated
                ? Component.literal(font.plainSubstrByWidth(text.getString(), available - font.width(ELLIPSIS)) + ELLIPSIS)
                : text;
        }
        return cache;
    }

    private boolean isTruncated() {
        getOrBuildComponent();
        return isTruncated;
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }
}

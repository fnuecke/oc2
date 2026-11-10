/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class UiRenderer {
    private static final int TOOLTIP_WIDTH = 200;

    // --------------------------------------------------------------------- //

    private final GuiGraphics graphics;
    private final Theme theme;
    private final Font font;

    // --------------------------------------------------------------------- //

    public UiRenderer(final GuiGraphics graphics, final Theme theme) {
        this.graphics = graphics;
        this.theme = theme;
        this.font = Minecraft.getInstance().font;
    }

    // --------------------------------------------------------------------- //

    public Theme theme() {
        return theme;
    }

    public GuiGraphics graphics() {
        return graphics;
    }

    public void sprite(final Identifier sprite, final int x, final int y, final int width, final int height) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
    }

    public void atlasSprite(final TextureAtlasSprite sprite, final int x, final int y, final int width, final int height, final int argb) {
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height, argb);
    }

    public void text(final Component text, final int x, final int y, final int argb, final boolean shadow) {
        graphics.drawString(font, text, x, y, argb, shadow);
    }

    public void pushScissor(final int x0, final int y0, final int x1, final int y1) {
        graphics.enableScissor(x0, y0, x1, y1);
    }

    public void popScissor() {
        graphics.disableScissor();
    }

    public void tooltip(final List<Component> lines, final int mouseX, final int mouseY) {
        final List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (final Component line : lines) {
            wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
        }
        graphics.setTooltipForNextFrame(font, wrapped, mouseX, mouseY);
    }
}

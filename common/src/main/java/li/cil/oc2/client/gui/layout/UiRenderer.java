/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
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

    public void sprite(final ResourceLocation sprite, final int x, final int y, final int width, final int height) {
        graphics.blitSprite(sprite, x, y, width, height);
    }

    public void atlasSprite(final TextureAtlasSprite sprite, final int x, final int y, final int width, final int height, final int argb) {
        graphics.blit(x, y, 0, width, height, sprite,
            FastColor.ARGB32.red(argb) / 255f, FastColor.ARGB32.green(argb) / 255f,
            FastColor.ARGB32.blue(argb) / 255f, FastColor.ARGB32.alpha(argb) / 255f);
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

    public void tooltip(final List<Component> lines) {
        final Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }
        final List<FormattedCharSequence> wrapped = new ArrayList<>();
        for (final Component line : lines) {
            wrapped.addAll(font.split(line, TOOLTIP_WIDTH));
        }
        screen.setTooltipForNextRenderPass(wrapped, DefaultTooltipPositioner.INSTANCE, false);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.layout;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import javax.annotation.Nullable;

public abstract class UiScreen extends Screen {
    private final Theme theme;
    @Nullable
    private Window window;

    // --------------------------------------------------------------------- //

    protected UiScreen(final Component title, final Theme theme) {
        super(title);
        this.theme = theme;
    }

    // --------------------------------------------------------------------- //

    @Override
    public void render(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        final Window window = window();
        if (window.isStale()) {
            window.layout(width, height);
        }

        super.render(graphics, mouseX, mouseY, partialTicks);

        final UiRenderer renderer = new UiRenderer(graphics, theme);
        window.drawForeground(renderer, mouseX, mouseY);
        window.drawTooltip(renderer, mouseX, mouseY);
    }

    @Override
    public void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        renderTransparentBackground(graphics);
        window().drawBackground(new UiRenderer(graphics, theme), mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // --------------------------------------------------------------------- //

    protected abstract Window createWindow();

    @Override
    protected void init() {
        super.init();
        if (window == null) {
            window = createWindow();
        }
        window.layout(width, height);
        window.widgets().forEach(this::addRenderableWidget);
    }

    // --------------------------------------------------------------------- //

    private Window window() {
        if (window == null) {
            throw new IllegalStateException("Screen was not initialized.");
        }
        return window;
    }
}

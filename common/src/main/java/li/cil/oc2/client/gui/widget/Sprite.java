/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;

public final class Sprite {
    public final Texture texture;
    public final int width, height;
    public final int u0, v0;

    // ------------------------------------------------------------- //

    public Sprite(final Texture texture) {
        this.texture = texture;
        this.width = texture.width;
        this.height = texture.height;
        this.u0 = 0;
        this.v0 = 0;
    }

    public Sprite(final Texture texture, final int width, final int height, final int u0, final int v0) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.u0 = u0;
        this.v0 = v0;
    }

    // ------------------------------------------------------------- //

    public void draw(final GuiGraphics graphics, final int x, final int y) {
        draw(graphics, x, y, 0, 0);
    }

    public void draw(final GuiGraphics graphics, final int x, final int y, final int uOffset, final int vOffset) {
        blit(graphics, x, y, u0 + uOffset, v0 + vOffset, width, height);
    }

    public void drawFillY(final GuiGraphics graphics, final int x, final int y, final float value) {
        final int h = (int) (this.height * Mth.clamp(value, 0, 1));
        blit(graphics, x, y + height - h, u0, v0 + height - h, width, h);
    }

    private void blit(final GuiGraphics graphics, final int x, final int y, final int u0, final int v0, final int width, final int height) {
        // GuiGraphics binds the texture itself, so the explicit Texture.bind() is gone.
        graphics.blit(texture.location, x, y, u0, v0, width, height, texture.width, texture.height);
    }
}

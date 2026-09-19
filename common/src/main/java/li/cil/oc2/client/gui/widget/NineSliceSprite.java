/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

public final class NineSliceSprite {
    public final Texture texture;
    public final int width, height;
    public final int u0, v0;
    public final int border;

    // --------------------------------------------------------------------- //

    public NineSliceSprite(final Texture texture, final int width, final int height, final int u0, final int v0, final int border) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.u0 = u0;
        this.v0 = v0;
        this.border = border;
    }

    // --------------------------------------------------------------------- //

    public int minimumWidth() {
        return border * 2;
    }

    public int minimumHeight() {
        return border * 2;
    }

    public void draw(final GuiGraphics graphics, final int x, final int y, final int width, final int height) {
        final int innerWidth = Math.max(0, width - border * 2);
        final int innerHeight = Math.max(0, height - border * 2);
        final int sourceInnerWidth = this.width - border * 2;
        final int sourceInnerHeight = this.height - border * 2;
        final int right = x + width - border;
        final int bottom = y + height - border;
        final int uRight = u0 + this.width - border;
        final int vBottom = v0 + this.height - border;

        blit(graphics, x, y, u0, v0, border, border);
        blit(graphics, right, y, uRight, v0, border, border);
        blit(graphics, x, bottom, u0, vBottom, border, border);
        blit(graphics, right, bottom, uRight, vBottom, border, border);

        tile(graphics, x + border, y, innerWidth, border, u0 + border, v0, sourceInnerWidth, border);
        tile(graphics, x + border, bottom, innerWidth, border, u0 + border, vBottom, sourceInnerWidth, border);
        tile(graphics, x, y + border, border, innerHeight, u0, v0 + border, border, sourceInnerHeight);
        tile(graphics, right, y + border, border, innerHeight, uRight, v0 + border, border, sourceInnerHeight);
        tile(graphics, x + border, y + border, innerWidth, innerHeight,
            u0 + border, v0 + border, sourceInnerWidth, sourceInnerHeight);
    }

    // --------------------------------------------------------------------- //

    private void tile(final GuiGraphics graphics, final int x, final int y, final int width, final int height,
                      final int u, final int v, final int tileWidth, final int tileHeight) {
        for (int offsetY = 0; offsetY < height; offsetY += tileHeight) {
            final int sliceHeight = Math.min(tileHeight, height - offsetY);
            for (int offsetX = 0; offsetX < width; offsetX += tileWidth) {
                blit(graphics, x + offsetX, y + offsetY, u, v, Math.min(tileWidth, width - offsetX), sliceHeight);
            }
        }
    }

    private void blit(final GuiGraphics graphics, final int x, final int y, final int u, final int v, final int width, final int height) {
        graphics.blit(texture.location, x, y, u, v, width, height, texture.width, texture.height);
    }
}

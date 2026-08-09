/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.gui.widget;

import net.minecraft.client.gui.GuiGraphics;

public abstract class ToggleImageButton extends ImageButton {
    private final Sprite activeImage;
    private boolean isToggled;

    ///////////////////////////////////////////////////////////////////

    public ToggleImageButton(
        final int x, final int y,
        final int width, final int height,
        final Sprite baseImage,
        final Sprite pressedImage,
        final Sprite activeImage) {
        super(x, y, width, height, baseImage, pressedImage);
        this.activeImage = activeImage;
    }

    ///////////////////////////////////////////////////////////////////

    public boolean isToggled() {
        return isToggled;
    }

    public void setToggled(final boolean value) {
        isToggled = value;
    }

    @Override
    protected void renderBackground(final GuiGraphics graphics, final int mouseX, final int mouseY, final float partialTicks) {
        super.renderBackground(graphics, mouseX, mouseY, partialTicks);
        if (isToggled()) {
            activeImage.draw(graphics, x, y);
        }
    }
}

package li.cil.oc2.client.gui.widget;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.util.ResourceLocation;

public final class Sprite extends AbstractGui {
    public final ResourceLocation image;
    public final int textureSize;
    public final int width, height;
    public final int u0, v0;

    public Sprite(final ResourceLocation atlas, final int textureSize, final int width, final int height, final int u0, final int v0) {
        this.image = atlas;
        this.textureSize = textureSize;
        this.width = width;
        this.height = height;
        this.u0 = u0;
        this.v0 = v0;
    }

    public void draw(final MatrixStack stack, final int x, final int y) {
        Minecraft.getInstance().getTextureManager().bind(image);
        blit(stack, x, y, u0, v0, width, height, textureSize, textureSize);
    }

    public void drawFillY(final MatrixStack stack, final int x, final int y, final float value) {
        Minecraft.getInstance().getTextureManager().bind(image);
        final int h = (int) (this.height * value);
        blit(stack, x, y + (height - h), u0, v0 + (height - h), width, h, textureSize, textureSize);
    }
}

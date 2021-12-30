package li.cil.oc2.client.renderer.font;

import li.cil.manual.api.prefab.renderer.BitmapFontRenderer;
import li.cil.manual.api.render.FontRenderer;
import li.cil.oc2.api.API;
import net.minecraft.resources.ResourceLocation;

public final class MonospaceFontRenderer extends BitmapFontRenderer {
    public static final FontRenderer INSTANCE = new MonospaceFontRenderer();

    private static final ResourceLocation LOCATION_FONT_TEXTURE = new ResourceLocation(API.MOD_ID, "textures/font/monospace.png");
    private static final String CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

    @Override
    public int lineHeight() {
        return 9;
    }

    @Override
    protected CharSequence getCharacters() {
        return CHARS;
    }

    @Override
    protected ResourceLocation getTextureLocation() {
        return LOCATION_FONT_TEXTURE;
    }

    @Override
    protected int getResolution() {
        return 96;
    }

    @Override
    protected int getGapU() {
        return 0;
    }

    @Override
    protected int getGapV() {
        return 0;
    }

    @Override
    protected int charWidth() {
        return 6;
    }
}

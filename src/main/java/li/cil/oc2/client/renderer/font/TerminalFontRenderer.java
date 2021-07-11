package li.cil.oc2.client.renderer.font;

import li.cil.manual.api.prefab.renderer.BitmapFontRenderer;
import li.cil.manual.api.render.FontRenderer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.Terminal;
import net.minecraft.util.ResourceLocation;

public final class TerminalFontRenderer extends BitmapFontRenderer {
    public static final FontRenderer INSTANCE = new TerminalFontRenderer();

    private static final ResourceLocation LOCATION_FONT_TEXTURE = new ResourceLocation(API.MOD_ID, "textures/font/terminus_simple.png");
    private static final String CHARS = " !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";

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
        return 128;
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
        return Terminal.CHAR_WIDTH;
    }

    @Override
    public int lineHeight() {
        return Terminal.CHAR_HEIGHT;
    }
}

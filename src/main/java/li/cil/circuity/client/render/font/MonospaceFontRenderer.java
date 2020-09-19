package li.cil.circuity.client.render.font;

import com.mojang.blaze3d.platform.GlStateManager;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import li.cil.circuity.api.CircuityAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

public final class MonospaceFontRenderer implements FontRenderer {
    public static final FontRenderer INSTANCE = new MonospaceFontRenderer();

    private static final ResourceLocation LOCATION_FONT_TEXTURE = new ResourceLocation(CircuityAPI.MOD_ID, "textures/font/monospace.png");
    private static final String CHARS = "\u263a\u263b\u2665\u2666\u2663\u2660\u2022\u25d8\u25cb\u25d9\u2642\u2640\u266a\u266b\u263c\u25ba\u25c4\u2195\u203c\u00b6\u00a7\u25ac\u21a8\u2191\u2193\u2192\u2190\u221f\u2194\u25b2\u25bc !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~\u2302\u00c7\u00fc\u00e9\u00e2\u00e4\u00e0\u00e5\u00e7\u00ea\u00eb\u00e8\u00ef\u00ee\u00ec\u00c4\u00c5\u00c9\u00e6\u00c6\u00f4\u00f6\u00f2\u00fb\u00f9\u00ff\u00d6\u00dc\u00a2\u00a3\u00a5\u20a7\u0192\u00e1\u00ed\u00f3\u00fa\u00f1\u00d1\u00aa\u00ba\u00bf\u2310\u00ac\u00bd\u00bc\u00a1\u00ab\u00bb\u2591\u2592\u2593\u2502\u2524\u2561\u2562\u2556\u2555\u2563\u2551\u2557\u255d\u255c\u255b\u2510\u2514\u2534\u252c\u251c\u2500\u253c\u255e\u255f\u255a\u2554\u2569\u2566\u2560\u2550\u256c\u2567\u2568\u2564\u2565\u2559\u2558\u2552\u2553\u256b\u256a\u2518\u250c\u2588\u2584\u258c\u2590\u2580\u03b1\u00df\u0393\u03c0\u03a3\u03c3\u00b5\u03c4\u03a6\u0398\u03a9\u03b4\u221e\u03c6\u03b5\u2229\u2261\u00b1\u2265\u2264\u2320\u2321\u00f7\u2248\u00b0\u2219\u00b7\u221a\u207f\u00b2\u25a0";
    private static final int TEXTURE_RESOLUTION = 256;

    private final Int2IntMap CHAR_MAP;

    private final int COLUMNS = TEXTURE_RESOLUTION / getCharWidth();
    private final float U_SIZE = getCharWidth() / (float) TEXTURE_RESOLUTION;
    private final float V_SIZE = getCharHeight() / (float) TEXTURE_RESOLUTION;
    private final float U_STEP = getCharWidth() / (float) TEXTURE_RESOLUTION;
    private final float V_STEP = getCharHeight() / (float) TEXTURE_RESOLUTION;

    private MonospaceFontRenderer() {
        CHAR_MAP = new Int2IntOpenHashMap();
        final CharSequence chars = CHARS;
        for (int index = 0; index < chars.length(); index++) {
            CHAR_MAP.put(chars.charAt(index), index);
        }
    }

    public void drawString(final Matrix4f matrix, final CharSequence value) {
        drawString(matrix, value, value.length());
    }

    public void drawString(final Matrix4f matrix, final CharSequence value, final int maxChars) {
        GlStateManager.depthMask(false);

        Minecraft.getInstance().getTextureManager().bindTexture(LOCATION_FONT_TEXTURE);

        final Tessellator tessellator = Tessellator.getInstance();
        final BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);

        float tx = 0f;
        final int end = Math.min(maxChars, value.length());
        for (int i = 0; i < end; i++) {
            final char ch = value.charAt(i);
            drawChar(matrix, tx, ch, buffer);
            tx += getCharWidth();
        }

        tessellator.draw();

        GlStateManager.depthMask(true);
    }

    // --------------------------------------------------------------------- //
    // FontRenderer

    @Override
    public int getCharWidth() {
        return 9;
    }

    @Override
    public int getCharHeight() {
        return 16;
    }

    // --------------------------------------------------------------------- //

    private void drawChar(final Matrix4f matrix, final float x, final char ch, final BufferBuilder buffer) {
        if (Character.isWhitespace(ch) || Character.isISOControl(ch)) {
            return;
        }
        final int index = getCharIndex(ch);

        final int column = index % COLUMNS;
        final int row = index / COLUMNS;
        final float u = column * U_STEP;
        final float v = row * V_STEP;

        buffer.pos(matrix, x, getCharHeight(), 0).tex(u, v + V_SIZE).endVertex();
        buffer.pos(matrix, x + getCharWidth(), getCharHeight(), 0).tex(u + U_SIZE, v + V_SIZE).endVertex();
        buffer.pos(matrix, x + getCharWidth(), 0, 0).tex(u + U_SIZE, v).endVertex();
        buffer.pos(matrix, x, 0, 0).tex(u, v).endVertex();
    }

    private int getCharIndex(final char ch) {
        if (!CHAR_MAP.containsKey(ch)) {
            return CHAR_MAP.get('?');
        }
        return CHAR_MAP.get(ch);
    }
}

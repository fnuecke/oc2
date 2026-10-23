/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.device.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

import static li.cil.oc2.common.vm.device.Terminal.*;

@Environment(EnvType.CLIENT)
public final class TerminalRenderer {
    private static final Identifier LOCATION_FONT_TEXTURE = Identifier.fromNamespaceAndPath(API.MOD_ID, "textures/font/terminus.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final float ONE_OVER_TEXTURE_RESOLUTION = 1.0f / TEXTURE_RESOLUTION;
    private static final float CHAR_WIDTH_IN_UV = CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION;
    private static final float CHAR_HEIGHT_IN_UV = CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION;
    private static final int TEXTURE_COLUMNS = 16;
    private static final int TEXTURE_BOLD_SHIFT = TEXTURE_COLUMNS; // Bold chars are in right half of texture.
    private static final int WHITE_CELL = 0x7F;
    private static final int WHITE_CELL_X = (WHITE_CELL % TEXTURE_COLUMNS + TEXTURE_BOLD_SHIFT) * CHAR_WIDTH + CHAR_WIDTH / 2;
    private static final int WHITE_CELL_Y = WHITE_CELL / TEXTURE_COLUMNS * CHAR_HEIGHT + CHAR_HEIGHT / 2;
    private static final float WHITE_U = WHITE_CELL_X * ONE_OVER_TEXTURE_RESOLUTION;
    private static final float WHITE_V = WHITE_CELL_Y * ONE_OVER_TEXTURE_RESOLUTION;

    private static final int[] COLORS = {
        0x010101, // Black
        0xEE3322, // Red
        0x33CC44, // Green
        0xDDBB11, // Yellow
        0x1188EE, // Blue
        0xDD33CC, // Magenta
        0x22BBCC, // Cyan
        0xCCCCCC, // White
    };

    private static final int[] BRIGHT_COLORS = {
        0x555555, // Black
        0xFF7766, // Red
        0x66FF77, // Green
        0xFFEE55, // Yellow
        0x55BBFF, // Blue
        0xFF88FF, // Magenta
        0x66EEFF, // Cyan
        0xFFFFFF, // White
    };

    private static final int[] DIM_COLORS = {
        0x010101, // Black
        0x772211, // Red
        0x116622, // Green
        0x886611, // Yellow
        0x115588, // Blue
        0x771177, // Magenta
        0x116677, // Cyan
        0x777777, // White
    };

    // --------------------------------------------------------------------- //

    private final Terminal terminal;

    private final Matrix4f rowMatrix = new Matrix4f();

    // --------------------------------------------------------------------- //

    public TerminalRenderer(final Terminal terminal) {
        this.terminal = terminal;
    }

    // --------------------------------------------------------------------- //

    public void buildBody(final VertexConsumer builder) {
        for (int row = 0; row < HEIGHT; row++) {
            rowMatrix.translation(0, row * CHAR_HEIGHT, 0);
            renderBackground(rowMatrix, builder, row);
            renderForeground(rowMatrix, builder, row);
        }
    }

    // --------------------------------------------------------------------- //

    private void renderBackground(final Matrix4f matrix, final VertexConsumer buffer, final int row) {
        // State tracking for drawing background quads spanning multiple characters.
        float backgroundStartX = -1;
        int backgroundColor = 0;

        float tx = 0f;
        for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
            final int cell = terminal.getCell(index);

            final int background = resolveColor(getBackgroundColorIndex(cell), isBackgroundBright(cell), isDim(cell));

            final boolean hadBackground = backgroundStartX >= 0;
            final boolean hasBackground = background != resolveColor(0, false, isDim(cell));
            if (!hadBackground && hasBackground) {
                backgroundStartX = tx;
                backgroundColor = background;
            } else if (hadBackground && (!hasBackground || backgroundColor != background)) {
                renderBackground(matrix, buffer, backgroundStartX, tx, backgroundColor);

                if (hasBackground) {
                    backgroundStartX = tx;
                    backgroundColor = background;
                } else {
                    backgroundStartX = -1;
                }
            }

            tx += CHAR_WIDTH;
        }

        if (backgroundStartX >= 0) {
            renderBackground(matrix, buffer, backgroundStartX, tx, backgroundColor);
        }
    }

    private void renderBackground(final Matrix4f matrix, final VertexConsumer buffer, final float x0, final float x1, final int color) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        buffer.addVertex(matrix, x0, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        buffer.addVertex(matrix, x1, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        buffer.addVertex(matrix, x1, 0, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        buffer.addVertex(matrix, x0, 0, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
    }

    private void renderForeground(final Matrix4f matrix, final VertexConsumer buffer, final int row) {
        float tx = 0f;
        for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
            final int cell = terminal.getCell(index);

            if (isVisible(cell)) {
                final int foreground = resolveColor(getForegroundColorIndex(cell), isForegroundBright(cell), isDim(cell));

                renderForeground(matrix, buffer, tx, getCharacter(cell), foreground, isBold(cell), isUnderline(cell));
            }

            tx += CHAR_WIDTH;
        }
    }

    private static int resolveColor(final int index, final boolean isBright, final boolean isDim) {
        if (isDim) {
            return DIM_COLORS[index];
        }
        if (isBright) {
            return BRIGHT_COLORS[index];
        }
        return COLORS[index];
    }

    private void renderForeground(final Matrix4f matrix, final VertexConsumer buffer, final float offset, final int character, final int color, final boolean isBold, final boolean isUnderline) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        if (isPrintableCharacter((char) character)) {
            final int x = character % TEXTURE_COLUMNS + (isBold ? TEXTURE_BOLD_SHIFT : 0);
            final int y = character / TEXTURE_COLUMNS;
            final float u0 = x * CHAR_WIDTH_IN_UV;
            final float u1 = (x + 1) * CHAR_WIDTH_IN_UV;
            final float v0 = y * CHAR_HEIGHT_IN_UV;
            final float v1 = (y + 1) * CHAR_HEIGHT_IN_UV;

            buffer.addVertex(matrix, offset, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(u0, v1);
            buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(u1, v1);
            buffer.addVertex(matrix, offset + CHAR_WIDTH, 0, 0).setColor(r, g, b, 1).setUv(u1, v0);
            buffer.addVertex(matrix, offset, 0, 0).setColor(r, g, b, 1).setUv(u0, v0);
        }

        if (isUnderline) {
            buffer.addVertex(matrix, offset, CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
            buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
            buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
            buffer.addVertex(matrix, offset, CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        }
    }

    void buildCursor(final Matrix4f matrix, final VertexConsumer builder) {
        final int cursorX = terminal.getCursorX();
        final int cursorY = terminal.getCursorY();
        final int cell = terminal.getCell(cursorY * WIDTH + cursorX);

        final int blockColor = resolveColor(getForegroundColorIndex(cell), isForegroundBright(cell), isDim(cell));
        final int glyphColor = resolveColor(getBackgroundColorIndex(cell), isBackgroundBright(cell), isDim(cell));

        rowMatrix.set(matrix).translate(cursorX * CHAR_WIDTH, cursorY * CHAR_HEIGHT, 0);

        renderBackground(rowMatrix, builder, 0, CHAR_WIDTH, blockColor);
        if (isVisible(cell)) {
            renderForeground(rowMatrix, builder, 0, getCharacter(cell), glyphColor, isBold(cell), isUnderline(cell));
        }
    }

    static Identifier getFontTexture() {
        return LOCATION_FONT_TEXTURE;
    }

    static boolean shouldRenderCursor(final Terminal terminal) {
        return terminal.isCursorVisible() && (System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500;
    }

    @VisibleForTesting
    static boolean isPrintableCharacter(final char ch) {
        return ch > 0 && ch != ' ' && (ch < 0x7F || ch > 0xA0);
    }
}

/* SPDX-License-Identifier: MIT */

package li.cil.oc2.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import li.cil.oc2.api.API;
import li.cil.oc2.common.vm.Terminal;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;

import static li.cil.oc2.common.vm.Terminal.*;

@Environment(EnvType.CLIENT)
public final class TerminalRenderer implements Terminal.Listener, AutoCloseable {
    private static final ResourceLocation LOCATION_FONT_TEXTURE = ResourceLocation.fromNamespaceAndPath(API.MOD_ID, "textures/font/terminus.png");
    private static final int TEXTURE_RESOLUTION = 256;
    private static final float ONE_OVER_TEXTURE_RESOLUTION = 1.0f / TEXTURE_RESOLUTION;
    private static final float CHAR_WIDTH_IN_UV = CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION;
    private static final float CHAR_HEIGHT_IN_UV = CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION;
    private static final int TEXTURE_COLUMNS = 16;
    private static final int TEXTURE_BOLD_SHIFT = TEXTURE_COLUMNS; // Bold chars are in right half of texture.
    private static final float WHITE_U = (TEXTURE_RESOLUTION - 1.5f) * ONE_OVER_TEXTURE_RESOLUTION;
    private static final float WHITE_V = 1.5f * ONE_OVER_TEXTURE_RESOLUTION;

    private static final int[] COLORS = {
        0x010101, // Black
        0xEE3322, // Red
        0x33DD44, // Green
        0xFFCC11, // Yellow
        0x1188EE, // Blue
        0xDD33CC, // Magenta
        0x22CCDD, // Cyan
        0xEEEEEE, // White
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

    private static final int CURSOR_COLOR = COLORS[COLOR_WHITE];

    // --------------------------------------------------------------------- //

    private final Terminal terminal;

    @Nullable
    private VertexBuffer buffer;

    private final AtomicBoolean dirty = new AtomicBoolean(true);

    private final Matrix4f rowMatrix = new Matrix4f();
    private final Matrix4f modelViewMatrix = new Matrix4f();

    // --------------------------------------------------------------------- //

    public TerminalRenderer(final Terminal terminal) {
        this.terminal = terminal;
        terminal.addListener(this);
    }

    // --------------------------------------------------------------------- //

    public void render(final PoseStack stack, final Matrix4f modelViewBase, final Matrix4f projectionMatrix) {
        validateMesh();
        renderBuffer(stack, modelViewBase, projectionMatrix);

        if ((System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500) {
            renderCursor(stack);
        }
    }

    @Override
    public void handleTerminalChanged() {
        dirty.set(true);
    }

    @Override
    public void close() {
        terminal.removeListener(this);
        releaseBuffer();
    }

    // --------------------------------------------------------------------- //

    private void releaseBuffer() {
        if (buffer != null) {
            buffer.close();
            buffer = null;
        }
    }

    // --------------------------------------------------------------------- //

    private void renderBuffer(final PoseStack stack, final Matrix4f modelViewBase, final Matrix4f projectionMatrix) {
        if (buffer == null) {
            return;
        }

        ShaderInstance shader = ModShaders.getTerminalShader();
        if (shader == null) {
            // Shouldn't really happen, but just in case.
            shader = GameRenderer.getPositionTexColorShader();
        }
        if (shader == null) {
            return;
        }

        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderTexture(0, LOCATION_FONT_TEXTURE);

        modelViewMatrix.set(modelViewBase).mul(stack.last().pose());

        buffer.bind();
        buffer.drawWithShader(modelViewMatrix, projectionMatrix, shader);
        VertexBuffer.unbind();

        RenderSystem.disableBlend();
        RenderSystem.depthMask(true);
    }

    private void validateMesh() {
        if (!dirty.getAndSet(false)) {
            return;
        }

        final BufferBuilder builder = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (int row = 0; row < HEIGHT; row++) {
            rowMatrix.translation(0, row * CHAR_HEIGHT, 0);
            renderBackground(rowMatrix, builder, row);
            renderForeground(rowMatrix, builder, row);
        }

        final MeshData mesh = builder.build();
        if (mesh == null) {
            // Nothing visible at all; drop the buffer rather than leave the last frame on screen.
            releaseBuffer();
            return;
        }

        if (buffer == null) {
            buffer = new VertexBuffer(VertexBuffer.Usage.STATIC);
        }

        buffer.bind();
        buffer.upload(mesh);
        VertexBuffer.unbind();
    }

    private void renderBackground(final Matrix4f matrix, final BufferBuilder buffer, final int row) {
        // State tracking for drawing background quads spanning multiple characters.
        float backgroundStartX = -1;
        int backgroundColor = 0;

        float tx = 0f;
        for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
            final int cell = terminal.getCell(index);

            if (isHidden(cell)) continue;

            final int[] palette = isDim(cell) ? DIM_COLORS : COLORS;
            final int background = palette[getBackgroundColorIndex(cell)];

            final boolean hadBackground = backgroundStartX >= 0;
            final boolean hasBackground = background != palette[0];
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

    private void renderBackground(final Matrix4f matrix, final BufferBuilder buffer, final float x0, final float x1, final int color) {
        final float r = ((color >> 16) & 0xFF) / 255f;
        final float g = ((color >> 8) & 0xFF) / 255f;
        final float b = (color & 0xFF) / 255f;

        buffer.addVertex(matrix, x0, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        buffer.addVertex(matrix, x1, CHAR_HEIGHT, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        buffer.addVertex(matrix, x1, 0, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        buffer.addVertex(matrix, x0, 0, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
    }

    private void renderForeground(final Matrix4f matrix, final BufferBuilder buffer, final int row) {
        float tx = 0f;
        for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
            final int cell = terminal.getCell(index);

            if (isHidden(cell)) continue;

            final int[] palette = isDim(cell) ? DIM_COLORS : COLORS;
            final int foreground = palette[getForegroundColorIndex(cell)];

            renderForeground(matrix, buffer, tx, getCharacter(cell), foreground, isBold(cell), isUnderline(cell));

            tx += CHAR_WIDTH;
        }
    }

    private void renderForeground(final Matrix4f matrix, final BufferBuilder buffer, final float offset, final int character, final int color, final boolean isBold, final boolean isUnderline) {
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
            buffer.addVertex(matrix, offset, CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
            buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT - 3, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
            buffer.addVertex(matrix, offset + CHAR_WIDTH, CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
            buffer.addVertex(matrix, offset, CHAR_HEIGHT - 2, 0).setColor(r, g, b, 1).setUv(WHITE_U, WHITE_V);
        }
    }

    private void renderCursor(final PoseStack stack) {
        final int cursorX = terminal.getCursorX();
        final int cursorY = terminal.getCursorY();
        if (cursorX < 0 || cursorX >= WIDTH || cursorY < 0 || cursorY >= HEIGHT) {
            return;
        }

        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        stack.pushPose();
        stack.translate(cursorX * CHAR_WIDTH, cursorY * CHAR_HEIGHT, 0);

        final Matrix4f matrix = stack.last().pose();
        final BufferBuilder buffer = Tesselator.getInstance()
            .begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        final float r = ((CURSOR_COLOR >> 16) & 0xFF) / 255f;
        final float g = ((CURSOR_COLOR >> 8) & 0xFF) / 255f;
        final float b = (CURSOR_COLOR & 0xFF) / 255f;

        buffer.addVertex(matrix, 0, CHAR_HEIGHT, 0).setColor(r, g, b, 1);
        buffer.addVertex(matrix, CHAR_WIDTH, CHAR_HEIGHT, 0).setColor(r, g, b, 1);
        buffer.addVertex(matrix, CHAR_WIDTH, 0, 0).setColor(r, g, b, 1);
        buffer.addVertex(matrix, 0, 0, 0).setColor(r, g, b, 1);

        BufferUploader.drawWithShader(buffer.buildOrThrow());

        stack.popPose();

        RenderSystem.depthMask(true);
    }

    private static boolean isPrintableCharacter(final char ch) {
        return ch == 0 ||
            (ch > ' ' && ch <= '~') ||
            ch >= 177;
    }
}

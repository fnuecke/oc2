package li.cil.oc2.common.vm;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import li.cil.oc2.api.API;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.state.properties.NoteBlockInstrument;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

// Implements a couple of control sequences from here: https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_sequences
@Serialized
public final class Terminal {
    private static final int TAB_WIDTH = 4;
    private static final int WIDTH = 80, HEIGHT = 24;

    private static final int COLOR_BLACK = 0;
    private static final int COLOR_RED = 1;
    private static final int COLOR_GREEN = 2;
    private static final int COLOR_YELLOW = 3;
    private static final int COLOR_BLUE = 4;
    private static final int COLOR_MAGENTA = 5;
    private static final int COLOR_CYAN = 6;
    private static final int COLOR_WHITE = 7;

    private static final int CHAR_WIDTH = 8;
    private static final int CHAR_HEIGHT = 16;

    private static final int COLOR_MASK = 0b111;
    private static final int COLOR_FOREGROUND_SHIFT = 3;

    private static final int STYLE_BOLD_MASK = 1;
    private static final int STYLE_DIM_MASK = 1 << 1;
    private static final int STYLE_UNDERLINE_MASK = 1 << 2;
    private static final int STYLE_BLINK_MASK = 1 << 3;
    private static final int STYLE_INVERT_MASK = 1 << 4;
    private static final int STYLE_HIDDEN_MASK = 1 << 5;

    // Default style: no modifiers, white foreground, black background.
    private static final byte DEFAULT_COLORS = COLOR_WHITE << COLOR_FOREGROUND_SHIFT;
    private static final byte DEFAULT_STYLE = 0;

    public enum State { // Must be public for serialization.
        NORMAL, // Currently reading characters normally.
        ESCAPE, // Last character was ESC, figure out what kind next.
        SEQUENCE, // Know what sequence we have, now parsing it.
    }

    private final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    private final byte[] buffer = new byte[WIDTH * HEIGHT];
    private final byte[] colors = new byte[WIDTH * HEIGHT];
    private final byte[] styles = new byte[WIDTH * HEIGHT];
    private State state = State.NORMAL;
    private final int[] args = new int[4];
    private int argCount = 0;
    private int x, y;
    private int savedX, savedY;

    // Color info packed into one byte for compact storage
    // 0-2: background color (index)
    // 3-5: foreground color (index)
    private byte color = DEFAULT_COLORS;
    // Style info packed into one byte for compact storage
    private byte style = DEFAULT_STYLE;

    // Rendering data for client
    private final transient AtomicInteger dirty = new AtomicInteger(-1);
    private transient Object renderer;
    private transient boolean displayOnly; // Set on client to not send responses to status requests.

    public Terminal() {
        clear();
    }

    public void setDisplayOnly(final boolean value) {
        displayOnly = value;
    }

    public int getWidth() {
        return WIDTH * CHAR_WIDTH;
    }

    public int getHeight() {
        return HEIGHT * CHAR_HEIGHT;
    }

    @OnlyIn(Dist.CLIENT)
    public void render(final MatrixStack stack) {
        if (renderer == null) {
            renderer = new Renderer(this);
        }
        ((Renderer) renderer).render(dirty, stack);
    }

    public synchronized int readInput() {
        if (input.isEmpty()) {
            return -1;
        } else {
            return input.dequeueByte() & 0xFF;
        }
    }

    @Nullable
    public synchronized ByteBuffer getInput() {
        if (input.isEmpty()) {
            return null;
        } else {
            final ByteBuffer buffer = ByteBuffer.allocate(input.size());
            while (!input.isEmpty()) {
                buffer.put(input.dequeueByte());
            }
            buffer.flip();
            return buffer;
        }
    }

    public synchronized void putInput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            input.enqueue(values.get());
        }
    }

    public synchronized void putOutput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            putOutput(values.get());
        }
    }

    public synchronized void putInput(final byte value) {
        input.enqueue(value);
    }

    public void putOutput(final byte value) {
        final char ch = (char) value;
        switch (state) {
            case NORMAL: {
                switch (value) {
                    case (byte) '\r': {
                        setCursorPos(0, y);
                        break;
                    }
                    case (byte) '\n': {
                        putNewLine();
                        break;
                    }
                    case (byte) '\t': {
                        if (x + TAB_WIDTH > WIDTH) {
                            setCursorPos(0, y);
                            putNewLine();
                        } else {
                            setCursorPos(x + TAB_WIDTH - (x % TAB_WIDTH), y);
                        }
                        break;
                    }
                    case (byte) '\b': {
                        setCursorPos(x - 1, y);
                        break;
                    }
                    case 7: {
                        Minecraft.getInstance().getSoundHandler().play(SimpleSound.master(NoteBlockInstrument.PLING.getSound(), 1));
                        break;
                    }
                    case 27: {
                        state = State.ESCAPE;
                        break;
                    }
                    default: {
                        if (!Character.isISOControl(ch)) {
                            putChar(ch);
                        }
                        break;
                    }
                }
                break;
            }

            case ESCAPE: {
                if (ch == '[') {
                    Arrays.fill(args, (byte) 0);
                    argCount = 0;
                    state = State.SEQUENCE;
                } else {
                    state = State.NORMAL;
                }
                break;
            }

            case SEQUENCE: {
                if (ch >= '0' && ch <= '9') {
                    if (argCount < args.length) {
                        final int digit = ch - '0';
                        if (args[argCount] < (Integer.MAX_VALUE - digit) / 10) {
                            args[argCount] = args[argCount] * 10 + digit;
                        } else {
                            args[argCount] = Integer.MAX_VALUE;
                        }
                    }
                } else {
                    if (argCount < args.length) {
                        argCount++;
                    }

                    if (ch == ';' || ch == '?') {
                        break;
                    }

                    state = State.NORMAL;

                    switch (ch) {
                        case 'A': { // Cursor Up
                            setCursorPos(x, y - Math.max(1, args[0]));
                            break;
                        }
                        case 'B': { // Cursor Down
                            setCursorPos(x, y + Math.max(1, args[0]));
                            break;
                        }
                        case 'C': { // Cursor Forward
                            setCursorPos(x + Math.max(1, args[0]), y);
                            break;
                        }
                        case 'D': { // Cursor Back
                            setCursorPos(x - Math.max(1, args[0]), y);
                            break;
                        }
                        case 'E': { // Cursor Next Line
                            setCursorPos(0, y + Math.min(1, args[0]));
                            break;
                        }
                        case 'F': { // Cursor Previous Line
                            setCursorPos(0, y - Math.min(1, args[0]));
                            break;
                        }
                        case 'G': { // Cursor Horizontal Absolute
                            setCursorPos(args[0] - 1, y);
                            break;
                        }
                        case 'f': // Don't care about terminal mode fanciness so just alias.
                        case 'H': { // Cursor Position
                            setCursorPos(args[1] - 1, args[0] - 1);
                            break;
                        }
                        case 'J': { // Erase in Display
                            if (args[0] == 0) { // Cursor and down
                                clearLine(y, x, WIDTH);
                                for (int iy = y + 1; iy < HEIGHT; iy++) {
                                    clearLine(iy);
                                }
                            } else if (args[0] == 1) { // Cursor and up
                                clearLine(y, 0, x + 1);
                                for (int iy = 0; iy < y; iy++) {
                                    clearLine(iy);
                                }
                            } else if (args[0] == 2) { // Everything
                                clear();
                            }
                            break;
                        }
                        case 'K': { // Erase in Line
                            if (args[0] == 0) { // Cursor and right
                                clearLine(y, x, WIDTH);
                            } else if (args[0] == 1) { // Cursor and left
                                clearLine(y, 0, x + 1);
                            } else if (args[0] == 2) { // ...entirely
                                clearLine(y);
                            }
                            break;
                        }
                        // S, T: Scroll Up/Down. We don't have scrollback.
                        case 'm': { // Select Graphic Rendition
                            for (int i = 0; i < argCount; i++) {
                                final int arg = args[i];
                                selectStyle(arg);
                            }
                            break;
                        }
                        case 'n': { // Device Status Report
                            switch (args[0]) {
                                case 5: { // Report console status
                                    if (!displayOnly) {
                                        putInput((byte) 27);
                                        for (final char i : "[0n".toCharArray()) {
                                            putInput((byte) i);
                                        }
                                    }
                                    break;
                                }
                                case 6: { // Report cursor position
                                    if (!displayOnly) {
                                        putInput((byte) 27);
                                        for (final char i : String.format("[%d;%dR", (y % HEIGHT) + 1, x + 1).toCharArray()) {
                                            putInput((byte) i);
                                        }
                                    }
                                    break;
                                }
                            }
                            break;
                        }
                        case 's': { // Save Current Cursor Position
                            savedX = x;
                            savedY = y;
                            break;
                        }
                        case 'u': { // Restore Saved Cursor Position
                            x = savedX;
                            y = savedY;
                            break;
                        }
                    }
                }
                break;
            }
        }
    }

    private void selectStyle(final int sgr) {
        switch (sgr) {
            case 0: { // Reset / Normal
                color = DEFAULT_COLORS;
                style = DEFAULT_STYLE;
                break;
            }
            case 1: { // Bold or increased intensity
                style |= STYLE_BOLD_MASK;
                break;
            }
            case 2: { // Faint or decreased intensity
                style |= STYLE_DIM_MASK;
                break;
            }
            case 4: { // Underline
                style |= STYLE_UNDERLINE_MASK;
                break;
            }
            case 5: { // Slow Blink
                style |= STYLE_BLINK_MASK;
                break;
            }
            case 7: { // Reverse video
                style |= STYLE_INVERT_MASK;
                break;
            }
            case 8: { // Conceal aka Hide
                style |= STYLE_HIDDEN_MASK;
                break;
            }
            case 22: { // Normal color or intensity
                style &= ~(STYLE_BOLD_MASK | STYLE_DIM_MASK);
                break;
            }
            case 24: { // Underline off
                style &= ~STYLE_UNDERLINE_MASK;
                break;
            }
            case 25: { // Blink off
                style &= ~STYLE_BLINK_MASK;
                break;
            }
            case 27: { // Reverse/invert off
                style &= ~STYLE_INVERT_MASK;
                break;
            }
            case 28: { // Reveal conceal off
                style &= ~STYLE_HIDDEN_MASK;
                break;
            }
            case 30:
            case 31:
            case 32:
            case 33:
            case 34:
            case 35:
            case 36:
            case 37: { // Set foreground color
                final int color = sgr - 30;
                this.color = (byte) ((this.color & ~(COLOR_MASK << COLOR_FOREGROUND_SHIFT)) | (color << COLOR_FOREGROUND_SHIFT));
                break;
            }
            case 40:
            case 41:
            case 42:
            case 43:
            case 44:
            case 45:
            case 46:
            case 47: { //–47 Set background color
                final int color = sgr - 40;
                this.color = (byte) ((this.color & ~COLOR_MASK) | color);
                break;
            }
        }
    }

    private void setCursorPos(final int x, final int y) {
        this.x = Math.max(0, Math.min(WIDTH - 1, x));
        this.y = Math.max(0, Math.min(HEIGHT - 1, y));
    }

    private void putChar(final char ch) {
        if (x >= WIDTH) {
            setCursorPos(0, y);
            putNewLine();
        }

        setChar(x, y, ch);
        x++;
    }

    private void setChar(final int x, final int y, final char ch) {
        final int index = x + y * WIDTH;
        if (buffer[index] == ch &&
            colors[index] == color &&
            styles[index] == style) {
            return;
        }

        buffer[index] = (byte) ch;
        colors[index] = color;
        styles[index] = style;
        dirty.accumulateAndGet(1 << y, (prev, next) -> prev | next);
    }

    private void clear() {
        Arrays.fill(buffer, (byte) ' ');
        Arrays.fill(colors, DEFAULT_COLORS);
        Arrays.fill(styles, DEFAULT_STYLE);
        dirty.set((1 << HEIGHT) - 1);
    }

    private void clearLine(final int y) {
        clearLine(y, 0, WIDTH);
    }

    private void clearLine(final int y, final int fromIndex, final int toIndex) {
        Arrays.fill(buffer, y * WIDTH + fromIndex, y * WIDTH + toIndex, (byte) ' ');
        Arrays.fill(colors, y * WIDTH + fromIndex, y * WIDTH + toIndex, DEFAULT_COLORS);
        Arrays.fill(styles, y * WIDTH + fromIndex, y * WIDTH + toIndex, DEFAULT_STYLE);
        dirty.accumulateAndGet(1 << y, (prev, next) -> prev | next);
    }

    private void putNewLine() {
        y++;
        if (y >= HEIGHT) {
            y = HEIGHT - 1;
            shiftUpOne();
        }
    }

    private void shiftUpOne() {
        System.arraycopy(buffer, WIDTH, buffer, 0, buffer.length - WIDTH);
        System.arraycopy(colors, WIDTH, colors, 0, colors.length - WIDTH);
        System.arraycopy(styles, WIDTH, styles, 0, styles.length - WIDTH);
        Arrays.fill(buffer, WIDTH * HEIGHT - WIDTH, WIDTH * HEIGHT, (byte) ' ');
        Arrays.fill(colors, WIDTH * HEIGHT - WIDTH, WIDTH * HEIGHT, DEFAULT_COLORS);
        Arrays.fill(styles, WIDTH * HEIGHT - WIDTH, WIDTH * HEIGHT, DEFAULT_STYLE);

        // Offset is baked into buffers so we must rebuild them all.
        dirty.set(-1);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Renderer {
        private static final ResourceLocation LOCATION_FONT_TEXTURE = new ResourceLocation(API.MOD_ID, "textures/font/terminus.png");
        private static final int TEXTURE_RESOLUTION = 256;
        private static final float ONE_OVER_TEXTURE_RESOLUTION = 1.0f / (float) TEXTURE_RESOLUTION;
        private static final int TEXTURE_COLUMNS = 16;
        private static final int TEXTURE_BOLD_SHIFT = TEXTURE_COLUMNS; // Bold chars are in right half of texture.

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

        private final Terminal terminal;

        private final Object[] lines = new Object[HEIGHT]; // Cached vertex buffers for rendering, untyped for server.
        private Object lastMatrix; // Untyped for server.

        public Renderer(final Terminal terminal) {
            this.terminal = terminal;
        }

        public void render(final AtomicInteger dirty, final MatrixStack stack) {
            validateLineCache(dirty, stack);
            renderBuffer();

            if ((System.currentTimeMillis() + terminal.hashCode()) % 1000 > 500) {
                renderCursor(stack);
            }
        }

        private void renderBuffer() {
            GlStateManager.depthMask(false);
            Minecraft.getInstance().getTextureManager().bindTexture(LOCATION_FONT_TEXTURE);

            final BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            for (final Object line : lines) {
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);
                buffer.setVertexState((BufferBuilder.State) line);
                buffer.finishDrawing();
                WorldVertexBufferUploader.draw(buffer);
            }

            GlStateManager.depthMask(true);
        }

        private void validateLineCache(final AtomicInteger dirty, final MatrixStack stack) {
            if (!Objects.equals(lastMatrix, stack.getLast().getMatrix())) {
                lastMatrix = stack.getLast().getMatrix();
                dirty.set(-1);
            }

            if (dirty.get() == 0) {
                return;
            }

            final BufferBuilder buffer = Tessellator.getInstance().getBuffer();

            final int mask = dirty.getAndSet(0);
            for (int row = 0; row < lines.length; row++) {
                if ((mask & (1 << row)) == 0) {
                    continue;
                }

                stack.push();
                stack.translate(0, row * CHAR_HEIGHT, 0);
                final Matrix4f matrix = stack.getLast().getMatrix();

                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR_TEX);

                float tx = 0f;
                for (int col = 0, index = row * WIDTH; col < WIDTH; col++, index++) {
                    final int character = terminal.buffer[index] & 0xFF;
                    final byte colors = terminal.colors[index];
                    final byte style = terminal.styles[index];

                    if ((style & STYLE_HIDDEN_MASK) != 0) continue;

                    final int x = character % TEXTURE_COLUMNS + ((style & STYLE_BOLD_MASK) != 0 ? TEXTURE_BOLD_SHIFT : 0);
                    final int y = character / TEXTURE_COLUMNS;
                    final float u0 = x * (CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION);
                    final float u1 = (x + 1) * (CHAR_WIDTH * ONE_OVER_TEXTURE_RESOLUTION);
                    final float v0 = y * (CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION);
                    final float v1 = (y + 1) * (CHAR_HEIGHT * ONE_OVER_TEXTURE_RESOLUTION);

                    final int[] palette = (style & STYLE_DIM_MASK) != 0 ? DIM_COLORS : COLORS;

                    final int foreground = palette[(colors >> COLOR_FOREGROUND_SHIFT) & COLOR_MASK];
                    final int background = palette[colors & COLOR_MASK];

                    final int color = (style & STYLE_INVERT_MASK) != 0 ? background : foreground;
                    final float r = ((color >> 16) & 0xFF) / 255f;
                    final float g = ((color >> 8) & 0xFF) / 255f;
                    final float b = ((color) & 0xFF) / 255f;

                    if (isPrintableCharacter((char) character)) {
                        buffer.pos(matrix, tx, CHAR_HEIGHT, 0).color(r, g, b, 1).tex(u0, v1).endVertex();
                        buffer.pos(matrix, tx + CHAR_WIDTH, CHAR_HEIGHT, 0).color(r, g, b, 1).tex(u1, v1).endVertex();
                        buffer.pos(matrix, tx + CHAR_WIDTH, 0, 0).color(r, g, b, 1).tex(u1, v0).endVertex();
                        buffer.pos(matrix, tx, 0, 0).color(r, g, b, 1).tex(u0, v0).endVertex();
                    }

                    if ((style & STYLE_UNDERLINE_MASK) != 0) {
                        final float ulu = (TEXTURE_RESOLUTION - 1) / (float) TEXTURE_RESOLUTION;
                        final float ulv = 1 / (float) TEXTURE_RESOLUTION;

                        buffer.pos(matrix, tx, CHAR_HEIGHT - 3, 0).color(r, g, b, 1).tex(ulu, ulv).endVertex();
                        buffer.pos(matrix, tx + CHAR_WIDTH, CHAR_HEIGHT - 3, 0).color(r, g, b, 1).tex(ulu, ulv).endVertex();
                        buffer.pos(matrix, tx + CHAR_WIDTH, CHAR_HEIGHT - 2, 0).color(r, g, b, 1).tex(ulu, ulv).endVertex();
                        buffer.pos(matrix, tx, CHAR_HEIGHT - 2, 0).color(r, g, b, 1).tex(ulu, ulv).endVertex();
                    }

                    tx += CHAR_WIDTH;
                }

                lines[row] = buffer.getVertexState();
                buffer.finishDrawing();
                buffer.discard();

                stack.pop();
            }
        }

        private void renderCursor(final MatrixStack stack) {
            if (terminal.x < 0 || terminal.x >= WIDTH || terminal.y < 0 || terminal.y >= HEIGHT) {
                return;
            }

            GlStateManager.depthMask(false);
            RenderSystem.disableTexture();

            stack.push();
            stack.translate(terminal.x * CHAR_WIDTH, terminal.y * CHAR_HEIGHT, 0);

            final Matrix4f matrix = stack.getLast().getMatrix();
            final BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

            final int foreground = COLORS[COLOR_WHITE];
            final float r = ((foreground >> 16) & 0xFF) / 255f;
            final float g = ((foreground >> 8) & 0xFF) / 255f;
            final float b = ((foreground) & 0xFF) / 255f;

            buffer.pos(matrix, 0, CHAR_HEIGHT, 0).color(r, g, b, 1).endVertex();
            buffer.pos(matrix, CHAR_WIDTH, CHAR_HEIGHT, 0).color(r, g, b, 1).endVertex();
            buffer.pos(matrix, CHAR_WIDTH, 0, 0).color(r, g, b, 1).endVertex();
            buffer.pos(matrix, 0, 0, 0).color(r, g, b, 1).endVertex();

            buffer.finishDrawing();
            WorldVertexBufferUploader.draw(buffer);

            stack.pop();

            RenderSystem.enableTexture();
            GlStateManager.depthMask(true);
        }

        private static boolean isPrintableCharacter(final char ch) {
            return ch == 0 ||
                   (ch > ' ' && ch <= '~') ||
                   ch >= 177;
        }
    }
}

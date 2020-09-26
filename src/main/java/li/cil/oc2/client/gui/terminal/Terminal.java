package li.cil.oc2.client.gui.terminal;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.oc2.client.render.font.FontRenderer;
import li.cil.oc2.client.render.font.MonospaceFontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SimpleSound;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Matrix4f;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.state.properties.NoteBlockInstrument;
import org.lwjgl.opengl.GL11;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

// Implements a couple of control sequences from here: https://en.wikipedia.org/wiki/ANSI_escape_code#CSI_sequences
public final class Terminal {
    private static final int TAB_WIDTH = 4;
    private static final int WIDTH = 80, HEIGHT = 25;

    private enum State {
        NORMAL, // Currently reading characters normally.
        ESCAPE, // Last character was ESC, figure out what kind next.
        SEQUENCE, // Know what sequence we have, now parsing it.
    }

    private final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    private final char[] buffer = new char[WIDTH * HEIGHT];
    private State state = State.NORMAL;
    private final int[] args = new int[4];
    private int argCount = 0;
    private int x, y;
    private int savedX, savedY;

    private final String[] lines = new String[HEIGHT]; // Cached strings for rendering.
    private final AtomicInteger dirty = new AtomicInteger(-1);

    public Terminal() {
        Arrays.fill(buffer, ' ');
    }

    public int getWidth() {
        return WIDTH * MonospaceFontRenderer.INSTANCE.getCharWidth();
    }

    public int getHeight() {
        return HEIGHT * MonospaceFontRenderer.INSTANCE.getCharHeight();
    }

    public void render(final MatrixStack stack) {
        final FontRenderer fontRenderer = MonospaceFontRenderer.INSTANCE;

        validateLineCache();

        for (int i = 0; i < lines.length; i++) {
            stack.push();
            stack.translate(0, i * fontRenderer.getCharHeight(), 0);
            fontRenderer.drawString(stack.getLast().getMatrix(), lines[i]);
            stack.pop();
        }

        if (System.currentTimeMillis() % 1000 > 500) {
            renderCursor(stack);
        }
    }

    public synchronized int readInput() {
        if (input.isEmpty()) {
            return -1;
        } else {
            return input.dequeueByte() & 0xFF;
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
                        // m: Select Graphic Rendition. TODO
                        case 'n': { // Device Status Report
                            for (final char i : String.format("ESC[%d;%dR", WIDTH, HEIGHT).toCharArray()) {
                                putInput((byte) i);
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

    private void renderCursor(final MatrixStack stack) {
        final FontRenderer fontRenderer = MonospaceFontRenderer.INSTANCE;

        GlStateManager.depthMask(false);
        RenderSystem.disableTexture();

        stack.push();
        stack.translate(x * fontRenderer.getCharWidth(), y * fontRenderer.getCharHeight(), 0);

        final Matrix4f matrix = stack.getLast().getMatrix();
        final BufferBuilder buffer = Tessellator.getInstance().getBuffer();

        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        buffer.pos(matrix, 0, fontRenderer.getCharHeight(), 0).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(matrix, fontRenderer.getCharWidth(), fontRenderer.getCharHeight(), 0).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(matrix, fontRenderer.getCharWidth(), 0, 0).color(1f, 1f, 1f, 1f).endVertex();
        buffer.pos(matrix, 0, 0, 0).color(1f, 1f, 1f, 1f).endVertex();

        buffer.finishDrawing();
        WorldVertexBufferUploader.draw(buffer);

        stack.pop();

        RenderSystem.enableTexture();
        GlStateManager.depthMask(true);
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
        setCursorPos(x + 1, y);
    }

    private void setChar(final int x, final int y, final char ch) {
        if (buffer[x + y * WIDTH] == ch) {
            return;
        }

        buffer[x + y * WIDTH] = ch;
        dirty.accumulateAndGet(1 << y, (prev, next) -> prev | next);
    }

    private void clear() {
        Arrays.fill(buffer, ' ');
        dirty.set((1 << HEIGHT) - 1);
    }

    private void clearLine(final int y) {
        clearLine(y, 0, WIDTH);
    }

    private void clearLine(final int y, final int fromIndex, final int toIndex) {
        Arrays.fill(buffer, y * WIDTH + fromIndex, y * WIDTH + toIndex, ' ');
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
        System.arraycopy(lines, 1, lines, 0, lines.length - 1);
        Arrays.fill(buffer, WIDTH * HEIGHT - WIDTH, WIDTH * HEIGHT, ' ');

        // Shift all dirty down one because we moved rows up one (up = lower indices).
        // Mark bottom-most line (highest index) as dirty.
        dirty.accumulateAndGet(1 << (HEIGHT - 1), (prev, next) -> (prev >>> 1) | next);
    }

    private void validateLineCache() {
        if (dirty.get() == 0) {
            return;
        }

        final int mask = dirty.getAndSet(0);
        for (int i = 0; i < lines.length; i++) {
            if ((mask & (1 << i)) != 0) {
                lines[i] = new String(buffer, i * WIDTH, WIDTH);
            }
        }
    }
}

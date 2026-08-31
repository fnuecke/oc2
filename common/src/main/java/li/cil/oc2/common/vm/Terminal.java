/* SPDX-License-Identifier: MIT */

package li.cil.oc2.common.vm;

import it.unimi.dsi.fastutil.bytes.ByteArrayFIFOQueue;
import li.cil.ceres.api.Serialized;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

// VT100 emulation: https://vt100.net/docs/vt100-ug/chapter3.html
@Serialized
public final class Terminal {
    private static final Logger LOGGER = LogManager.getLogger();

    public static final int WIDTH = 80, HEIGHT = 24;
    public static final int CHAR_WIDTH = 8;
    public static final int CHAR_HEIGHT = 16;
    public static final int COLOR_WHITE = Color.WHITE;

    private static final int TAB_WIDTH = 4;
    private static final char UNRENDERABLE = '?';
    private static final int MAX_EXPECTED_LISTENERS = 4;
    private static final int MAX_INPUT_SIZE = 4 * 1024;

    @SuppressWarnings("unused")
    private static final class Color {
        static final int BLACK = 0;
        static final int RED = 1;
        static final int GREEN = 2;
        static final int YELLOW = 3;
        static final int BLUE = 4;
        static final int MAGENTA = 5;
        static final int CYAN = 6;
        static final int WHITE = 7;
    }

    @SuppressWarnings("unused")
    private static final class Mode {
        static final int LNM = 20;    // Line Feed/New Line Mode
        static final int DECCKM = 1;  // Cursor key
        static final int DECANM = 2;  // ANSI/VT52
        static final int DECCOLM = 3; // Column
        static final int DECSCLM = 4; // Scrolling
        static final int DECSCNM = 5; // Screen
        static final int DECOM = 6;   // Origin
        static final int DECAWM = 7;  // Auto wrap
        static final int DECARM = 8;  // Auto-repeating
        static final int DECINLM = 9; // Interlace
    }

    private static final int COLOR_MASK = 0b111;
    private static final int COLOR_FOREGROUND_SHIFT = 3;

    private static final int STYLE_BOLD_MASK = 1;
    private static final int STYLE_DIM_MASK = 1 << 1;
    private static final int STYLE_UNDERLINE_MASK = 1 << 2;
    private static final int STYLE_BLINK_MASK = 1 << 3;
    private static final int STYLE_INVERT_MASK = 1 << 4;
    private static final int STYLE_HIDDEN_MASK = 1 << 5;

    // Default style: no modifiers, white foreground, black background.
    private static final byte DEFAULT_COLORS = Color.WHITE << COLOR_FOREGROUND_SHIFT;
    private static final byte DEFAULT_STYLE = 0;

    private static final int CELL_COLORS_SHIFT = 8;
    private static final int CELL_STYLE_SHIFT = 16;

    // --------------------------------------------------------------------- //

    public enum State { // Must be public for serialization.
        NORMAL, // Reading characters normally.
        ESCAPE, // Last character was ESC, figure out what kind next.
        SHIFT_IN_CHARACTER_SET, // Shift in character set.
        SHIFT_OUT_CHARACTER_SET, // Shift out character set.
        HASH, // Escape sequence with # intermediate.
        CONTROL_SEQUENCE, // Know what sequence we have, now parsing it.
    }

    public interface Listener {
        void handleTerminalChanged();
    }

    // --------------------------------------------------------------------- //

    private final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    private final byte[] buffer = new byte[WIDTH * HEIGHT];
    private final byte[] colors = new byte[WIDTH * HEIGHT];
    private final byte[] styles = new byte[WIDTH * HEIGHT];
    private final boolean[] tabs = new boolean[WIDTH];
    private State state = State.NORMAL;
    private final int[] args = new int[4];
    private int argCount;
    private int modes;
    private int scrollFirst, scrollLast = HEIGHT - 1;
    private int x, y;
    private int savedX, savedY;

    // Color info packed into one byte for compact storage
    // 0-2: background color (index)
    // 3-5: foreground color (index)
    private byte color;
    // Style info packed into one byte for compact storage
    private byte style;

    private final transient Set<Listener> listeners = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private transient boolean displayOnly; // Set on client to not send responses to status requests.
    private transient boolean hasPendingBell;

    // Persisted state of the decoder below, to resume decoding after load.
    private final byte[] utf8Pending = new byte[4];
    private int utf8PendingCount;

    private final transient CharsetDecoder utf8Decoder = StandardCharsets.UTF_8.newDecoder()
        .onMalformedInput(CodingErrorAction.REPLACE)
        .onUnmappableCharacter(CodingErrorAction.REPLACE);
    private final transient ByteBuffer utf8Input = ByteBuffer.allocate(8);
    private final transient CharBuffer utf8Output = CharBuffer.allocate(8);

    // --------------------------------------------------------------------- //

    public Terminal() {
        RIS();
    }

    // --------------------------------------------------------------------- //

    public int getWidth() {
        return WIDTH * CHAR_WIDTH;
    }

    public int getHeight() {
        return HEIGHT * CHAR_HEIGHT;
    }

    @Environment(EnvType.CLIENT)
    public void setDisplayOnly(final boolean value) {
        displayOnly = value;
    }

    public void addListener(final Listener listener) {
        listeners.add(listener);

        // Runtime quasi-assert to avoid leaking listeners again...
        if (listeners.size() > MAX_EXPECTED_LISTENERS) {
            LOGGER.warn("Terminal has {} listeners; one is likely not being removed.", listeners.size());
        }
    }

    public void removeListener(final Listener listener) {
        listeners.remove(listener);
    }

    // --------------------------------------------------------------------- //

    public int getCursorX() {
        return x;
    }

    public int getCursorY() {
        return y;
    }

    public int getCell(final int index) {
        return (buffer[index] & 0xFF) | ((colors[index] & 0xFF) << CELL_COLORS_SHIFT) | ((styles[index] & 0xFF) << CELL_STYLE_SHIFT);
    }

    public static int getCharacter(final int cell) {
        return cell & 0xFF;
    }

    public static int getForegroundColorIndex(final int cell) {
        return isInverted(cell) ? backgroundColorIndexOf(cell) : foregroundColorIndexOf(cell);
    }

    public static int getBackgroundColorIndex(final int cell) {
        return isInverted(cell) ? foregroundColorIndexOf(cell) : backgroundColorIndexOf(cell);
    }

    public static boolean isBold(final int cell) {
        return (cell >> CELL_STYLE_SHIFT & STYLE_BOLD_MASK) != 0;
    }

    public static boolean isDim(final int cell) {
        return (cell >> CELL_STYLE_SHIFT & STYLE_DIM_MASK) != 0;
    }

    public static boolean isUnderline(final int cell) {
        return (cell >> CELL_STYLE_SHIFT & STYLE_UNDERLINE_MASK) != 0;
    }

    public static boolean isHidden(final int cell) {
        return (cell >> CELL_STYLE_SHIFT & STYLE_HIDDEN_MASK) != 0;
    }

    private static boolean isInverted(final int cell) {
        return (cell >> CELL_STYLE_SHIFT & STYLE_INVERT_MASK) != 0;
    }

    private static int foregroundColorIndexOf(final int cell) {
        return cell >> (CELL_COLORS_SHIFT + COLOR_FOREGROUND_SHIFT) & COLOR_MASK;
    }

    private static int backgroundColorIndexOf(final int cell) {
        return cell >> CELL_COLORS_SHIFT & COLOR_MASK;
    }

    // --------------------------------------------------------------------- //

    public boolean consumePendingBell() {
        if (!hasPendingBell) {
            return false;
        }

        hasPendingBell = false;
        return true;
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
        while (values.hasRemaining() && input.size() < MAX_INPUT_SIZE) {
            input.enqueue(values.get());
        }
    }

    public synchronized void putOutput(final ByteBuffer values) {
        while (values.hasRemaining()) {
            putOutput(values.get());
        }
    }

    public synchronized void putInput(final byte value) {
        if (input.size() < MAX_INPUT_SIZE) {
            input.enqueue(value);
        }
    }

    public synchronized void putOutput(final byte value) {
        final char ch = (char) value;
        switch (state) {
            case NORMAL -> {
                switch (value) {
                    case '\007' -> hasPendingBell = true;
                    case '\033' -> state = State.ESCAPE;
                    case '\016' -> {
                    } // SO
                    case '\017' -> {
                    } // SI

                    case (byte) '\r' /* 015 */ -> setCursorPos(0, y);
                    case (byte) '\n' /* 012 */, '\013', '\014' -> {
                        if (getMode(Mode.LNM)) {
                            NEL();
                        } else {
                            IND();
                        }
                    }
                    case (byte) '\t' /* 011 */ -> {
                        if (x < WIDTH) {
                            do {
                                x++;
                            } while (x < WIDTH && !tabs[x]);
                        }
                    }
                    case (byte) '\b' /* 010 */ -> setCursorPos(Math.min(x, WIDTH - 1) - 1, y);

                    default -> putUtf8(value);
                }
            }
            case ESCAPE -> {
                if (ch == '[') { // Control Sequence Indicator
                    Arrays.fill(args, (byte) 0);
                    argCount = 0;
                    state = State.CONTROL_SEQUENCE;
                } else if (ch == '(') { // SCS – Select Character Set
                    state = State.SHIFT_IN_CHARACTER_SET;
                } else if (ch == ')') { // SCS – Select Character Set
                    state = State.SHIFT_OUT_CHARACTER_SET;
                } else if (ch == '#') { // # Intermediate
                    state = State.HASH;
                } else {
                    state = State.NORMAL;
                    switch (ch) {
                        case 'D' -> IND();   // IND – Index
                        case 'E' -> NEL();   // NEL – Next Line
                        case 'M' -> RI();    // RI – Reverse Index
                        case '7' -> DECSC(); // DECSC – Save Cursor (DEC Private)
                        case '8' -> DECRC(); // DECRC – Restore Cursor (DEC Private)
                        case 'H' -> HTS();   // HTS – Horizontal Tabulation Set
                        case 'c' -> RIS();   // RIS – Reset To Initial State
                        case '=' -> {
                        }      // DECKPAM – Keypad Application Mode (DEC Private)
                        case '>' -> {
                        }      // DECKPNM – Keypad Numeric Mode (DEC Private)
                    }
                }
            }
            case CONTROL_SEQUENCE -> {
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
                    if (ch == '?') {
                        break; // Ignore ? intermediate character.
                    }

                    if (argCount < args.length) {
                        argCount++;
                    }

                    if (ch == ';') {
                        break; // Keep going, we have another argument.
                    }

                    state = State.NORMAL;
                    switch (ch) {
                        case 'A' -> CUU(); // CUU - Cursor Up
                        case 'B' -> CUD(); // CUD – Cursor Down
                        case 'C' -> CUF(); // CUF – Cursor Forward
                        case 'D' -> CUB(); // CUB – Cursor Backward
                        case 'H' -> CUP(); // CUP - Cursor Position
                        case 'f' -> HVP(); // HVP – Horizontal and Vertical Position
                        case 'm' -> SGR(); // SGR – Select Graphic Rendition
                        case 'K' -> EL();  // EL – Erase In Line
                        case 'J' -> ED();  // ED – Erase In Display
                        case 'r' -> DECSTBM(); // DECSTBM – Set Top and Bottom Margins (DEC Private)
                        case 'g' -> TBC(); // TBC – Tabulation Clear
                        case 'h' -> SM();  // SM – Set Mode
                        case 'l' -> RM();  // RM – Reset Mode
                        case 'n' -> DSR(); // DSR – Device Status Report
                        case 'c' -> DA();  // DA – Device Attributes
                    }
                }
            }
            case SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET -> {
                state = State.NORMAL;
                switch (ch) {
                    case 'A' -> {
                    } // United Kingdom Set
                    case 'B' -> {
                    } // ASCII Set
                    case '0' -> {
                    } // Special Graphics
                    case '1' -> {
                    } // Alternate Character ROM Standard Character Set
                    case '2' -> {
                    } // Alternate Character ROM Special Graphics
                }
            }
            case HASH -> {
                state = State.NORMAL;
                switch (ch) {
                    case '3' -> {
                    } // Change this line to double-height top half (DECDHL)
                    case '4' -> {
                    } // Change this line to double-height bottom half (DECDHL)
                    case '5' -> {
                    } // Change this line to single-width single-height (DECSWL)
                    case '6' -> {
                    } // Change this line to double-width single-height (DECDWL)
                    case '8' -> { // Fill Screen with Es (DECALN)
                        Arrays.fill(buffer, (byte) 'E');
                        listeners.forEach(Listener::handleTerminalChanged);
                    }
                }
            }
        }
    }

    private void IND() {
        if (y >= scrollLast) {
            shiftUpOne();
        } else {
            setCursorPos(x, y + 1);
        }
    }

    private void NEL() {
        if (y >= scrollLast) {
            shiftUpOne();
            setCursorPos(0, y);
        } else {
            setCursorPos(0, y + 1);
        }
    }

    private void RI() {
        if (y <= scrollFirst) {
            shiftDownOne();
        } else {
            setCursorPos(0, y - 1);
        }
    }

    private void DECSC() {
        savedX = x;
        savedY = y;
    }

    private void DECRC() {
        x = savedX;
        y = savedY;
    }

    private void HTS() {
        if (x >= 0 && x < WIDTH) {
            tabs[x] = true;
        }
    }

    private void RIS() {
        utf8PendingCount = 0;
        color = DEFAULT_COLORS;
        style = DEFAULT_STYLE;
        clear();
        Arrays.fill(tabs, false);
        for (int i = 1; i < WIDTH; i++) {
            if (i % TAB_WIDTH == 0) {
                tabs[i] = true;
            }
        }
    }

    private void CUU() {
        setClampedCursorPos(x, y - Math.max(1, args[0]));
    }

    private void CUD() {
        setClampedCursorPos(x, y + Math.max(1, args[0]));
    }

    private void CUF() {
        setClampedCursorPos(x + Math.max(1, args[0]), y);
    }

    private void CUB() {
        setClampedCursorPos(x - Math.max(1, args[0]), y);
    }

    private void CUP() {
        setRelativeCursorPos(args[1] - 1, args[0] - 1);
    }

    private void HVP() {
        CUP();
    }

    private void SGR() {
        for (int i = 0; i < argCount; i++) {
            selectStyle(args[i]);
        }
    }

    private void EL() {
        switch (args[0]) {
            case 0 ->  // From cursor to end of line
                clearLine(y, cursorColumn(), WIDTH);
            case 1 ->  // From beginning of line to cursor
                clearLine(y, 0, cursorColumn() + 1);
            case 2 ->  // Entire line containing cursor
                clearLine(y);
        }
    }

    private void ED() {
        switch (args[0]) {
            case 0 -> {  // From cursor to end of screen
                clearLine(y, cursorColumn(), WIDTH);
                for (int iy = y + 1; iy < HEIGHT; iy++) {
                    clearLine(iy);
                }
            }
            case 1 -> {  // From beginning of screen to cursor
                for (int iy = 0; iy < y; iy++) {
                    clearLine(iy);
                }
                clearLine(y, 0, cursorColumn() + 1);
            }
            case 2 ->  // Entire screen
                clear();
        }
    }

    private void DECSTBM() {
        final int first, last;
        if (argCount == 2) {
            first = args[0] - 1;
            last = args[1] - 1;
        } else {
            first = 0;
            last = HEIGHT - 1;
        }
        if (first < 0 || last > HEIGHT - 1 || last - first <= 0) {
            return;
        }
        scrollFirst = first; // to index
        scrollLast = last; // to index
        setRelativeCursorPos(0, 0); // send cursor home
    }

    private void TBC() {
        switch (args[0]) {
            case 0 -> { // Clear tab at current column
                if (x >= 0 && x < WIDTH) {
                    tabs[x] = false;
                }
            }
            case 3 -> // Clear all tabs
                Arrays.fill(tabs, false);
        }
    }

    private void SM() {
        for (int i = 0; i < argCount; i++) {
            final int mode = args[i];
            if (mode != 0) {
                setMode(mode);
            }
            if (mode == Mode.DECOM) {
                setRelativeCursorPos(0, 0);
            }
        }
    }

    private void RM() {
        for (int i = 0; i < argCount; i++) {
            final int mode = args[i];
            if (mode != 0) {
                resetMode(mode);
            }
            if (mode == Mode.DECOM) {
                setRelativeCursorPos(0, 0);
                clear();
            }
        }
    }

    private void DSR() {
        switch (args[0]) {
            case 5 -> // Report console status
                putResponse("\033[0n"); // Ready, No malfunctions detected
            case 6 -> { // Report cursor position
                if (getMode(Mode.DECOM)) {
                    putResponse(String.format("\033[%d;%dR", y - scrollFirst + 1, x + 1));
                } else {
                    putResponse(String.format("\033[%d;%dR", (y % HEIGHT) + 1, x + 1));
                }
            }
        }
    }

    private void DA() {
        putResponse("\033[?1;0c"); // No options.
    }

    private void setMode(final int mode) {
        modes |= 1 << mode;
    }

    private void resetMode(final int mode) {
        modes &= ~(1 << mode);
    }

    private boolean getMode(final int mode) {
        return (modes & (1 << mode)) != 0;
    }

    private void putResponse(final String value) {
        for (int i = 0; i < value.length(); i++) {
            putResponse((byte) value.charAt(i));
        }
    }

    private void putResponse(final byte value) {
        if (!displayOnly) {
            putInput(value);
        }
    }

    private void selectStyle(final int sgr) {
        switch (sgr) {
            case 0 -> { // Reset / Normal
                color = DEFAULT_COLORS;
                style = DEFAULT_STYLE;
            }
            case 1 -> // Bold or increased intensity
                style |= STYLE_BOLD_MASK;
            case 2 -> // Faint or decreased intensity
                style |= STYLE_DIM_MASK;
            case 4 -> // Underscore
                style |= STYLE_UNDERLINE_MASK;
            case 5 -> // Blink
                style |= STYLE_BLINK_MASK;
            case 7 -> // Negative (reverse) image
                style |= STYLE_INVERT_MASK;
            case 8 -> // Conceal aka Hide
                style |= STYLE_HIDDEN_MASK;
            case 22 -> // Normal color or intensity
                style &= ~(STYLE_BOLD_MASK | STYLE_DIM_MASK);
            case 24 -> // Underline off
                style &= ~STYLE_UNDERLINE_MASK;
            case 25 -> // Blink off
                style &= ~STYLE_BLINK_MASK;
            case 27 -> // Reverse/invert off
                style &= ~STYLE_INVERT_MASK;
            case 28 -> // Reveal conceal off
                style &= ~STYLE_HIDDEN_MASK;
            case 30, 31, 32, 33, 34, 35, 36, 37 -> { // Set foreground color
                final int color = sgr - 30;
                this.color = (byte) ((this.color & ~(COLOR_MASK << COLOR_FOREGROUND_SHIFT)) | (color << COLOR_FOREGROUND_SHIFT));
            }
            case 40, 41, 42, 43, 44, 45, 46, 47 -> { //–47 Set background color
                final int color = sgr - 40;
                this.color = (byte) ((this.color & ~COLOR_MASK) | color);
            }
        }
    }

    private void setRelativeCursorPos(final int x, final int y) {
        if (getMode(Mode.DECOM)) {
            setCursorPos(x, Math.min(scrollFirst + y, scrollLast));
        } else {
            setCursorPos(x, y);
        }
    }

    private void setClampedCursorPos(final int x, final int y) {
        setCursorPos(x, Math.clamp(y, scrollFirst, scrollLast));
    }

    private void setCursorPos(final int x, final int y) {
        this.x = Math.clamp(x, 0, WIDTH - 1);
        this.y = Math.clamp(y, 0, HEIGHT - 1);
    }

    private int cursorColumn() {
        return Math.min(x, WIDTH - 1);
    }

    private void putUtf8(final byte value) {
        utf8Input.clear();
        utf8Input.put(utf8Pending, 0, utf8PendingCount);
        utf8Input.put(value);
        utf8Input.flip();

        utf8Output.clear();
        utf8Decoder.decode(utf8Input, utf8Output, false);

        utf8PendingCount = utf8Input.remaining();
        utf8Input.get(utf8Pending, 0, utf8PendingCount);

        utf8Output.flip();
        while (utf8Output.hasRemaining()) {
            final char ch = utf8Output.get();
            putChar(ch <= 0xFF ? ch : UNRENDERABLE);
        }
    }

    private void putChar(final char ch) {
        if (Character.isISOControl(ch))
            return;

        if (x >= WIDTH) {
            if (getMode(Mode.DECAWM)) {
                NEL();
            } else {
                setCursorPos(WIDTH - 1, y);
            }
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
        listeners.forEach(Listener::handleTerminalChanged);
    }

    private void clear() {
        Arrays.fill(buffer, (byte) ' ');
        Arrays.fill(colors, DEFAULT_COLORS);
        Arrays.fill(styles, DEFAULT_STYLE);
        listeners.forEach(Listener::handleTerminalChanged);
    }

    private void clearLine(final int y) {
        clearLine(y, 0, WIDTH);
    }

    private void clearLine(final int y, final int fromIndex, final int toIndex) {
        Arrays.fill(buffer, y * WIDTH + fromIndex, y * WIDTH + toIndex, (byte) ' ');
        Arrays.fill(colors, y * WIDTH + fromIndex, y * WIDTH + toIndex, DEFAULT_COLORS);
        Arrays.fill(styles, y * WIDTH + fromIndex, y * WIDTH + toIndex, DEFAULT_STYLE);
        listeners.forEach(Listener::handleTerminalChanged);
    }

    private void shiftUpOne() {
        shiftLines(scrollFirst + 1, scrollLast, -1);
    }

    private void shiftDownOne() {
        shiftLines(scrollFirst, scrollLast - 1, 1);
    }

    private void shiftLines(final int firstLine, final int lastLine, final int count) {
        if (count == 0)
            return;

        final int srcIndex = firstLine * WIDTH;
        final int charCount = (lastLine + 1) * WIDTH - srcIndex;
        final int dstIndex = srcIndex + count * WIDTH;

        System.arraycopy(buffer, srcIndex, buffer, dstIndex, charCount);
        System.arraycopy(colors, srcIndex, colors, dstIndex, charCount);
        System.arraycopy(styles, srcIndex, styles, dstIndex, charCount);

        final int clearIndex = count > 0 ? srcIndex : (dstIndex + charCount);
        final int clearCount = Math.abs(count * WIDTH);
        Arrays.fill(buffer, clearIndex, clearIndex + clearCount, (byte) ' ');
        // TODO Copy color and style from last line.
        Arrays.fill(colors, clearIndex, clearIndex + clearCount, DEFAULT_COLORS);
        Arrays.fill(styles, clearIndex, clearIndex + clearCount, DEFAULT_STYLE);

        listeners.forEach(Listener::handleTerminalChanged);
    }
}

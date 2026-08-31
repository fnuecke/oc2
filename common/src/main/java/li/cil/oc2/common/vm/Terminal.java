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
import java.util.Locale;
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

    private static final int TAB_WIDTH = 8;
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
    private static final int DEFAULT_PRIVATE_MODES = 1 << Mode.DECAWM;

    private static final int SGR_FOREGROUND_EXTENDED = 38;
    private static final int SGR_BACKGROUND_EXTENDED = 48;

    // DEC special graphics covers _ through ~; the font holds those glyphs at ch - ACS_FIRST.
    private static final char ACS_FIRST = 0x5F, ACS_LAST = 0x7E;

    private static final byte CAN = 0x18, SUB = 0x1A;
    private static final char DEL = 0x7F;

    // ECMA-48 byte ranges inside a control sequence. End on anything in 0x40 to 0x7E.
    private static final char CSI_INTERMEDIATE_FIRST = 0x20, CSI_INTERMEDIATE_LAST = 0x2F;
    private static final char CSI_UNHANDLED_PARAMETER_FIRST = 0x3A, CSI_UNHANDLED_PARAMETER_LAST = 0x3F;

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
        STRING, // Inside an OSC/DCS/PM/APC string, discarding until it terminates.
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
    private final int[] args = new int[8];
    private int argCount;
    private boolean ignoreSequence, isPrivateSequence;
    private boolean isG0Graphics, isG1Graphics, isShiftedOut;
    private int modes, privateModes;
    private int scrollFirst, scrollLast = HEIGHT - 1;
    private int x, y;
    private boolean isWrapPending;
    private int savedX, savedY;
    private byte savedColor, savedStyle;

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

    public boolean isCursorKeyApplicationMode() {
        return getPrivateMode(Mode.DECCKM);
    }

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
        if (value == '\033') {
            state = State.ESCAPE;
            return;
        }

        final char ch = (char) value;
        switch (state) {
            case NORMAL -> {
                if (!executeControl(value)) {
                    putUtf8(value);
                }
            }
            case ESCAPE -> {
                if (ch == '[') { // Control Sequence Indicator
                    Arrays.fill(args, (byte) 0);
                    argCount = 0;
                    ignoreSequence = isPrivateSequence = false;
                    state = State.CONTROL_SEQUENCE;
                } else if (ch == ']' || ch == 'P' || ch == '^' || ch == '_') { // OSC, DCS, PM, APC
                    state = State.STRING;
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
                if (value == CAN || value == SUB) {
                    state = State.NORMAL; // Both stop the sequence without displaying anything.
                } else if (ch < ' ' || ch == DEL) {
                    executeControl(value); // Handle controls right away.
                } else if (ch >= '0' && ch <= '9') {
                    if (argCount < args.length) {
                        final int digit = ch - '0';
                        if (args[argCount] < (Integer.MAX_VALUE - digit) / 10) {
                            args[argCount] = args[argCount] * 10 + digit;
                        } else {
                            args[argCount] = Integer.MAX_VALUE;
                        }
                    }
                } else if (ch == ';') {
                    if (argCount < args.length) {
                        argCount++;
                    }
                } else if (ch == '?') {
                    isPrivateSequence = true;
                } else if (ch >= CSI_UNHANDLED_PARAMETER_FIRST && ch <= CSI_UNHANDLED_PARAMETER_LAST
                    || ch >= CSI_INTERMEDIATE_FIRST && ch <= CSI_INTERMEDIATE_LAST) {
                    ignoreSequence = true; // We implement no sequence using these.
                } else {
                    if (argCount < args.length) {
                        argCount++;
                    }

                    state = State.NORMAL;
                    if (ignoreSequence) {
                        break;
                    }

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
            case STRING -> {
                if (value == '\007') { // The other terminator, ST, arrives as ESC.
                    state = State.NORMAL;
                }
            }
            case SHIFT_IN_CHARACTER_SET, SHIFT_OUT_CHARACTER_SET -> {
                // 0 is Special Graphics, 2 the alternate ROM's; A, B and 1 are text sets.
                final boolean isGraphics = ch == '0' || ch == '2';
                if (state == State.SHIFT_IN_CHARACTER_SET) {
                    isG0Graphics = isGraphics;
                } else {
                    isG1Graphics = isGraphics;
                }
                state = State.NORMAL;
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
        isWrapPending = false;
        if (y == scrollLast) {
            shiftUpOne();
        } else {
            setCursorPos(x, y + 1);
        }
    }

    private void NEL() {
        IND();
        setCursorPos(0, y);
    }

    private void RI() {
        isWrapPending = false;
        if (y == scrollFirst) {
            shiftDownOne();
        } else {
            setCursorPos(x, y - 1);
        }
    }

    private void DECSC() {
        savedX = x;
        savedY = y;
        savedColor = color;
        savedStyle = style;
    }

    private void DECRC() {
        setCursorPos(savedX, savedY);
        color = savedColor;
        style = savedStyle;
    }

    private void HTS() {
        if (x >= 0 && x < WIDTH) {
            tabs[x] = true;
        }
    }

    private void RIS() {
        utf8PendingCount = 0;
        state = State.NORMAL;
        argCount = 0;
        ignoreSequence = isPrivateSequence = false;
        isG0Graphics = isG1Graphics = isShiftedOut = false;
        isWrapPending = false;
        modes = 0;
        privateModes = DEFAULT_PRIVATE_MODES;
        color = DEFAULT_COLORS;
        style = DEFAULT_STYLE;
        savedColor = DEFAULT_COLORS;
        savedStyle = DEFAULT_STYLE;
        x = y = savedX = savedY = 0;
        scrollFirst = 0;
        scrollLast = HEIGHT - 1;
        clear();
        Arrays.fill(tabs, false);
        for (int i = 1; i < WIDTH; i++) {
            if (i % TAB_WIDTH == 0) {
                tabs[i] = true;
            }
        }
    }

    private void CUU() {
        final int top = y < scrollFirst ? 0 : scrollFirst;
        setCursorPos(x, Math.max(top, y - distance(args[0], HEIGHT)));
    }

    private void CUD() {
        final int bottom = y > scrollLast ? HEIGHT - 1 : scrollLast;
        setCursorPos(x, Math.min(bottom, y + distance(args[0], HEIGHT)));
    }

    private void CUF() {
        setCursorPos(x + distance(args[0], WIDTH), y);
    }

    private void CUB() {
        setCursorPos(x - distance(args[0], WIDTH), y);
    }

    private void CUP() {
        setRelativeCursorPos(Math.min(args[1], WIDTH) - 1, Math.min(args[0], HEIGHT) - 1);
    }

    private void HVP() {
        CUP();
    }

    private void SGR() {
        for (int i = 0; i < argCount; i++) {
            final int sgr = args[i];
            if (sgr == SGR_FOREGROUND_EXTENDED || sgr == SGR_BACKGROUND_EXTENDED) {
                i = selectExtendedColor(i);
            } else {
                selectStyle(sgr);
            }
        }
    }

    private void EL() {
        switch (args[0]) {
            case 0 ->  // From cursor to end of line
                clearLine(y, x, WIDTH);
            case 1 ->  // From beginning of line to cursor
                clearLine(y, 0, x + 1);
            case 2 ->  // Entire line containing cursor
                clearLine(y);
        }
    }

    private void ED() {
        switch (args[0]) {
            case 0 -> {  // From cursor to end of screen
                clearLine(y, x, WIDTH);
                for (int iy = y + 1; iy < HEIGHT; iy++) {
                    clearLine(iy);
                }
            }
            case 1 -> {  // From beginning of screen to cursor
                for (int iy = 0; iy < y; iy++) {
                    clearLine(iy);
                }
                clearLine(y, 0, x + 1);
            }
            case 2 ->  // Entire screen
                clear();
        }
    }

    private void DECSTBM() {
        final int first = args[0] > 0 ? args[0] - 1 : 0;
        final int last = argCount > 1 && args[1] > 0 ? args[1] - 1 : HEIGHT - 1;
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
                setMode(isPrivateSequence, mode);
            }
            if (isPrivateSequence && mode == Mode.DECOM) {
                setRelativeCursorPos(0, 0);
            }
        }
    }

    private void RM() {
        for (int i = 0; i < argCount; i++) {
            final int mode = args[i];
            if (mode != 0) {
                resetMode(isPrivateSequence, mode);
            }
            if (isPrivateSequence && mode == Mode.DECOM) {
                setRelativeCursorPos(0, 0);
            }
        }
    }

    private void DSR() {
        switch (args[0]) {
            case 5 -> // Report console status
                putResponse("\033[0n"); // Ready, No malfunctions detected
            case 6 -> { // Report cursor position
                final int row = getPrivateMode(Mode.DECOM) ? y - scrollFirst + 1 : y + 1;
                putResponse(String.format(Locale.ROOT, "\033[%d;%dR", row, x + 1));
            }
        }
    }

    private void DA() {
        putResponse("\033[?1;0c"); // No options.
    }

    private static int distance(final int argument, final int limit) {
        return Math.clamp(argument, 1, limit);
    }

    private boolean executeControl(final byte value) {
        switch (value) {
            case '\007' -> hasPendingBell = true;
            case '\016' -> isShiftedOut = true;  // SO – select G1 into GL
            case '\017' -> isShiftedOut = false; // SI – select G0 into GL

            case (byte) '\r' /* 015 */ -> setCursorPos(0, y);
            case (byte) '\n' /* 012 */, '\013', '\014' -> {
                if (getMode(Mode.LNM)) {
                    NEL();
                } else {
                    IND();
                }
            }
            case (byte) '\t' /* 011 */ -> {
                int stop = x;
                do {
                    stop++;
                } while (stop < WIDTH - 1 && !tabs[stop]);
                setCursorPos(stop, y);
            }
            case (byte) '\b' /* 010 */ -> setCursorPos(x - 1, y);

            default -> {
                return false;
            }
        }
        return true;
    }

    private void setMode(final boolean isPrivate, final int mode) {
        if (mode >= Integer.SIZE) {
            return;
        }
        if (isPrivate) {
            privateModes |= 1 << mode;
        } else {
            modes |= 1 << mode;
        }
    }

    private void resetMode(final boolean isPrivate, final int mode) {
        if (mode >= Integer.SIZE) {
            return;
        }
        if (isPrivate) {
            privateModes &= ~(1 << mode);
        } else {
            modes &= ~(1 << mode);
        }
    }

    private boolean getMode(final int mode) {
        return (modes & (1 << mode)) != 0;
    }

    private boolean getPrivateMode(final int mode) {
        return (privateModes & (1 << mode)) != 0;
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
            case 30, 31, 32, 33, 34, 35, 36, 37 -> // Set foreground color
                setColor(true, sgr - 30);
            case 39 -> // Default foreground color
                setColor(true, Color.WHITE);
            case 40, 41, 42, 43, 44, 45, 46, 47 -> //–47 Set background color
                setColor(false, sgr - 40);
            case 49 -> // Default background color
                setColor(false, Color.BLACK);
            // We only have the one intensity, so the bright colors are the regular ones.
            case 90, 91, 92, 93, 94, 95, 96, 97 -> // Set bright foreground color
                setColor(true, sgr - 90);
            case 100, 101, 102, 103, 104, 105, 106, 107 -> // Set bright background color
                setColor(false, sgr - 100);
        }
    }

    private int selectExtendedColor(final int index) {
        if (index + 1 >= argCount) {
            return index;
        }

        final boolean isForeground = args[index] == SGR_FOREGROUND_EXTENDED;
        switch (args[index + 1]) {
            case 5 -> { // ESC[38;5;n – 256 color palette
                if (index + 2 >= argCount) {
                    return argCount;
                }
                setColor(isForeground, paletteToColorIndex(args[index + 2]));
                return index + 2;
            }
            case 2 -> { // ESC[38;2;r;g;b – 24 bit color
                if (index + 4 >= argCount) {
                    return argCount;
                }
                setColor(isForeground, rgbToColorIndex(args[index + 2], args[index + 3], args[index + 4]));
                return index + 4;
            }
            default -> {
                return index + 1;
            }
        }
    }

    private static int paletteToColorIndex(final int palette) {
        if (palette < 16) { // Standard and bright colors.
            return palette & COLOR_MASK;
        }
        if (palette < 232) { // 6x6x6 color cube.
            final int index = palette - 16;
            return channelToColorMask(index / 36, 3, Color.RED)
                | channelToColorMask(index / 6 % 6, 3, Color.GREEN)
                | channelToColorMask(index % 6, 3, Color.BLUE);
        }
        return palette - 232 < 12 ? Color.BLACK : Color.WHITE; // Grayscale ramp.
    }

    private static int rgbToColorIndex(final int r, final int g, final int b) {
        return channelToColorMask(r, 128, Color.RED)
            | channelToColorMask(g, 128, Color.GREEN)
            | channelToColorMask(b, 128, Color.BLUE);
    }

    private static int channelToColorMask(final int value, final int threshold, final int mask) {
        return value >= threshold ? mask : 0;
    }

    private void setColor(final boolean isForeground, final int value) {
        if (isForeground) {
            color = (byte) ((color & ~(COLOR_MASK << COLOR_FOREGROUND_SHIFT)) | (value << COLOR_FOREGROUND_SHIFT));
        } else {
            color = (byte) ((color & ~COLOR_MASK) | value);
        }
    }

    private void setRelativeCursorPos(final int x, final int y) {
        if (getPrivateMode(Mode.DECOM)) {
            setCursorPos(x, Math.clamp(scrollFirst + y, scrollFirst, scrollLast));
        } else {
            setCursorPos(x, y);
        }
    }

    private void setCursorPos(final int x, final int y) {
        this.x = Math.clamp(x, 0, WIDTH - 1);
        this.y = Math.clamp(y, 0, HEIGHT - 1);
        isWrapPending = false;
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

        final char glyph = isGraphicsCharacter(ch) ? (char) (ch - ACS_FIRST) : ch;

        if (isWrapPending && getPrivateMode(Mode.DECAWM)) {
            NEL();
        }

        setChar(x, y, glyph);
        if (x < WIDTH - 1) {
            x++;
        } else {
            isWrapPending = true;
        }
    }

    private boolean isGraphicsCharacter(final char ch) {
        return ch >= ACS_FIRST && ch <= ACS_LAST && (isShiftedOut ? isG1Graphics : isG0Graphics);
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

    private byte eraseColor() {
        return (byte) ((DEFAULT_COLORS & ~COLOR_MASK) | (color & COLOR_MASK));
    }

    private void clear() {
        Arrays.fill(buffer, (byte) ' ');
        Arrays.fill(colors, eraseColor());
        Arrays.fill(styles, DEFAULT_STYLE);
        listeners.forEach(Listener::handleTerminalChanged);
    }

    private void clearLine(final int y) {
        clearLine(y, 0, WIDTH);
    }

    private void clearLine(final int y, final int fromIndex, final int toIndex) {
        Arrays.fill(buffer, y * WIDTH + fromIndex, y * WIDTH + toIndex, (byte) ' ');
        Arrays.fill(colors, y * WIDTH + fromIndex, y * WIDTH + toIndex, eraseColor());
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
        Arrays.fill(colors, clearIndex, clearIndex + clearCount, eraseColor());
        Arrays.fill(styles, clearIndex, clearIndex + clearCount, DEFAULT_STYLE);

        listeners.forEach(Listener::handleTerminalChanged);
    }
}

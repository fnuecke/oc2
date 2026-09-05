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
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    // The legacy encoding cannot say which button came up, so every release reports the same code.
    private static final int MOUSE_RELEASE_BUTTON = 3;
    private static final byte[] PASTE_START = "\033[200~".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PASTE_END = "\033[201~".getBytes(StandardCharsets.US_ASCII);

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
        static final int DECTCEM = 25; // Text cursor enable
        static final int ALT_BUFFER = 47;
        static final int ALT_BUFFER_CLEAR = 1047;
        static final int SAVE_CURSOR = 1048;
        static final int ALT_BUFFER_AND_CURSOR = 1049;
        static final int MOUSE_TRACKING = 1000;
        static final int MOUSE_SGR = 1006;
        static final int ALTERNATE_SCROLL = 1007;
        static final int BRACKETED_PASTE = 2004;
    }

    private static final int COLOR_MASK = 0b111;
    private static final int COLOR_FOREGROUND_SHIFT = 3;
    private static final int COLOR_BACKGROUND_BRIGHT_MASK = 1 << 6;
    private static final int COLOR_FOREGROUND_BRIGHT_MASK = 1 << 7;
    private static final int COLOR_BACKGROUND_MASK = COLOR_MASK | COLOR_BACKGROUND_BRIGHT_MASK;

    private static final int STYLE_BOLD_MASK = 1;
    private static final int STYLE_DIM_MASK = 1 << 1;
    private static final int STYLE_UNDERLINE_MASK = 1 << 2;
    private static final int STYLE_BLINK_MASK = 1 << 3;
    private static final int STYLE_INVERT_MASK = 1 << 4;
    private static final int STYLE_HIDDEN_MASK = 1 << 5;

    // Default style: no modifiers, white foreground, black background.
    private static final byte DEFAULT_COLORS = Color.WHITE << COLOR_FOREGROUND_SHIFT;
    private static final byte DEFAULT_STYLE = 0;
    private static final int DEFAULT_PRIVATE_MODES = 1 << Mode.DECAWM | 1 << Mode.DECTCEM;

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

    public interface Listener {
        void handleTerminalChanged();
    }

    @Serialized
    public static final class SavedScreen {
        public byte[] buffer, colors, styles;
        @Nullable
        public SavedCursor cursor;
    }

    @Serialized
    public static final class SavedCursor {
        public int x, y;
        public byte color = DEFAULT_COLORS, style = DEFAULT_STYLE;
        public boolean isG0Graphics, isG1Graphics, isShiftedOut, isOriginMode, isWrapPending;

        void reset() {
            x = y = 0;
            color = DEFAULT_COLORS;
            style = DEFAULT_STYLE;
            isG0Graphics = isG1Graphics = isShiftedOut = isOriginMode = isWrapPending = false;
        }
    }

    // --------------------------------------------------------------------- //

    private final ByteArrayFIFOQueue input = new ByteArrayFIFOQueue(32);
    private final byte[] buffer = new byte[WIDTH * HEIGHT];
    private final byte[] colors = new byte[WIDTH * HEIGHT];
    private final byte[] styles = new byte[WIDTH * HEIGHT];
    private final boolean[] tabs = new boolean[WIDTH];
    private final TerminalParser parser = new TerminalParser();
    private boolean isG0Graphics, isG1Graphics, isShiftedOut;
    private int modes, privateModes, highPrivateModes;
    private int scrollFirst, scrollLast = HEIGHT - 1;
    private int x, y;
    private boolean isWrapPending;
    private final SavedCursor savedCursor = new SavedCursor();
    @Nullable
    private SavedScreen savedScreen;

    // Color info packed into one byte for compact storage
    // 0-2: background color (index)
    // 3-5: foreground color (index)
    private byte color;
    // Style info packed into one byte for compact storage
    private byte style;

    private final transient Set<Listener> listeners = Collections.synchronizedSet(Collections.newSetFromMap(new WeakHashMap<>()));
    private transient boolean displayOnly; // Set on client to not send responses to status requests.
    private transient boolean hasPendingBell;

    private final transient TerminalParser.Sink sink = new ParserSink();

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

    public boolean isNewLineMode() {
        return getMode(Mode.LNM);
    }

    public boolean isCursorVisible() {
        return getPrivateMode(Mode.DECTCEM);
    }

    public boolean isBracketedPasteMode() {
        return getPrivateMode(Mode.BRACKETED_PASTE);
    }

    public boolean isMouseReportingEnabled() {
        return getPrivateMode(Mode.MOUSE_TRACKING);
    }

    boolean isAltBufferActive() {
        return savedScreen != null;
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

    public static boolean isForegroundBright(final int cell) {
        return isInverted(cell) ? backgroundBrightOf(cell) : foregroundBrightOf(cell);
    }

    public static boolean isBackgroundBright(final int cell) {
        return isInverted(cell) ? foregroundBrightOf(cell) : backgroundBrightOf(cell);
    }

    private static boolean foregroundBrightOf(final int cell) {
        return (cell >> CELL_COLORS_SHIFT & COLOR_FOREGROUND_BRIGHT_MASK) != 0;
    }

    private static boolean backgroundBrightOf(final int cell) {
        return (cell >> CELL_COLORS_SHIFT & COLOR_BACKGROUND_BRIGHT_MASK) != 0;
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

    public synchronized void putPaste(final String value) {
        final byte[] bytes = value.replace("\033", "").getBytes(StandardCharsets.UTF_8);
        if (!isBracketedPasteMode()) {
            putInput(ByteBuffer.wrap(bytes));
            return;
        }

        final int free = MAX_INPUT_SIZE - input.size() - PASTE_START.length - PASTE_END.length;
        if (free < 0) {
            return;
        }

        putInput(ByteBuffer.wrap(PASTE_START));
        putInput(ByteBuffer.wrap(bytes, 0, Math.min(bytes.length, free)));
        putInput(ByteBuffer.wrap(PASTE_END));
    }

    public synchronized boolean putScroll(final boolean isUp, final int lines) {
        if (!getPrivateMode(Mode.ALTERNATE_SCROLL) || !isAltBufferActive()) {
            return false;
        }

        final String sequence = (isCursorKeyApplicationMode() ? "\033O" : "\033[") + (isUp ? 'A' : 'B');
        for (int i = 0; i < lines; i++) {
            putInput(sequence);
        }

        return true;
    }

    public synchronized void putMouseEvent(final int button, final int column, final int row, final boolean isPressed) {
        if (getPrivateMode(Mode.MOUSE_SGR)) {
            putInput(String.format(Locale.ROOT, "\033[<%d;%d;%d%c", button, column + 1, row + 1, isPressed ? 'M' : 'm'));
        } else {
            putInput("\033[M");
            putInput((byte) (' ' + (isPressed ? button : MOUSE_RELEASE_BUTTON)));
            putInput((byte) (' ' + column + 1));
            putInput((byte) (' ' + row + 1));
        }
    }

    private void putInput(final String value) {
        for (int i = 0; i < value.length(); i++) {
            putInput((byte) value.charAt(i));
        }
    }

    public synchronized void putInput(final byte value) {
        if (input.size() < MAX_INPUT_SIZE) {
            input.enqueue(value);
        }
    }

    public synchronized void putOutput(final byte value) {
        parser.put(value, sink);
    }

    // --------------------------------------------------------------------- //

    private final class ParserSink implements TerminalParser.Sink {
        @Override
        public void print(final char ch) {
            putChar(ch <= 0xFF ? ch : UNRENDERABLE);
        }

        @Override
        public void execute(final byte control) {
            executeControl(control);
        }

        @Override
        public void escapeDispatch(final char intermediate, final char finalByte) {
            switch (intermediate) {
                case '(' -> isG0Graphics = isGraphicsSet(finalByte); // SCS – Select Character Set
                case ')' -> isG1Graphics = isGraphicsSet(finalByte); // SCS – Select Character Set
                case '#' -> {
                    if (finalByte == '8') { // DECALN – Screen Alignment Display
                        Arrays.fill(buffer, (byte) 'E');
                        listeners.forEach(Listener::handleTerminalChanged);
                    }
                }
                default -> {
                    switch (finalByte) {
                        case 'D' -> IND();   // IND – Index
                        case 'E' -> NEL();   // NEL – Next Line
                        case 'M' -> RI();    // RI – Reverse Index
                        case '7' -> DECSC(); // DECSC – Save Cursor (DEC Private)
                        case '8' -> DECRC(); // DECRC – Restore Cursor (DEC Private)
                        case 'H' -> HTS();   // HTS – Horizontal Tabulation Set
                        case 'c' -> RIS();   // RIS – Reset To Initial State
                    }
                }
            }
        }

        @Override
        public void controlSequenceDispatch(final char finalByte, final TerminalParser.Parameters parameters) {
            switch (finalByte) {
                case 'A' -> CUU(parameters);      // CUU – Cursor Up
                case 'B' -> CUD(parameters);      // CUD – Cursor Down
                case 'C' -> CUF(parameters);      // CUF – Cursor Forward
                case 'D' -> CUB(parameters);      // CUB – Cursor Backward
                case 'H', 'f' -> CUP(parameters); // CUP, HVP – Cursor Position
                case 'G' -> CHA(parameters);      // CHA – Cursor Character Absolute
                case 'd' -> VPA(parameters);      // VPA – Line Position Absolute
                case '@' -> ICH(parameters);      // ICH – Insert Character
                case 'P' -> DCH(parameters);      // DCH – Delete Character
                case 'X' -> ECH(parameters);      // ECH – Erase Character
                case 'L' -> IL(parameters);       // IL – Insert Line
                case 'M' -> DL(parameters);       // DL – Delete Line
                case 'S' -> SU(parameters);       // SU – Scroll Up
                case 'T' -> SD(parameters);       // SD – Scroll Down
                case 'm' -> SGR(parameters);      // SGR – Select Graphic Rendition
                case 'K' -> EL(parameters);       // EL – Erase In Line
                case 'J' -> ED(parameters);       // ED – Erase In Display
                case 'r' -> DECSTBM(parameters);  // DECSTBM – Set Top and Bottom Margins
                case 'g' -> TBC(parameters);      // TBC – Tabulation Clear
                case 'h' -> SM(parameters);       // SM – Set Mode
                case 'l' -> RM(parameters);       // RM – Reset Mode
                case 'n' -> DSR(parameters);      // DSR – Device Status Report
                case 'c' -> DA();                 // DA – Device Attributes
            }
        }
    }

    // --------------------------------------------------------------------- //

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
        saveCursor(savedCursor);
    }

    private void DECRC() {
        restoreCursor(savedCursor);
    }

    private void HTS() {
        if (x >= 0 && x < WIDTH) {
            tabs[x] = true;
        }
    }

    private void RIS() {
        parser.reset();
        isG0Graphics = isG1Graphics = isShiftedOut = false;
        isWrapPending = false;
        modes = 0;
        privateModes = DEFAULT_PRIVATE_MODES;
        highPrivateModes = 0;
        setMode(true, Mode.ALTERNATE_SCROLL);
        savedScreen = null;
        color = DEFAULT_COLORS;
        style = DEFAULT_STYLE;
        savedCursor.reset();
        x = y = 0;
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

    private void CUU(final TerminalParser.Parameters parameters) {
        final int top = y < scrollFirst ? 0 : scrollFirst;
        setCursorPos(x, Math.max(top, y - distance(parameters.get(0), HEIGHT)));
    }

    private void CUD(final TerminalParser.Parameters parameters) {
        final int bottom = y > scrollLast ? HEIGHT - 1 : scrollLast;
        setCursorPos(x, Math.min(bottom, y + distance(parameters.get(0), HEIGHT)));
    }

    private void CUF(final TerminalParser.Parameters parameters) {
        setCursorPos(x + distance(parameters.get(0), WIDTH), y);
    }

    private void CUB(final TerminalParser.Parameters parameters) {
        setCursorPos(x - distance(parameters.get(0), WIDTH), y);
    }

    private void CUP(final TerminalParser.Parameters parameters) {
        setRelativeCursorPos(Math.min(parameters.get(1), WIDTH) - 1, Math.min(parameters.get(0), HEIGHT) - 1);
    }

    private void CHA(final TerminalParser.Parameters parameters) {
        setCursorPos(Math.min(parameters.get(0), WIDTH) - 1, y);
    }

    private void VPA(final TerminalParser.Parameters parameters) {
        setRelativeCursorPos(x, Math.min(parameters.get(0), HEIGHT) - 1);
    }

    private void ICH(final TerminalParser.Parameters parameters) {
        shiftCells(clampToLineEnd(parameters));
    }

    private void DCH(final TerminalParser.Parameters parameters) {
        shiftCells(-clampToLineEnd(parameters));
    }

    private void ECH(final TerminalParser.Parameters parameters) {
        clearLine(y, x, x + clampToLineEnd(parameters));
    }

    private void IL(final TerminalParser.Parameters parameters) {
        if (y < scrollFirst || y > scrollLast) {
            return;
        }
        shiftRegion(y, scrollLast, distance(parameters.get(0), HEIGHT));
        setCursorPos(0, y);
    }

    private void DL(final TerminalParser.Parameters parameters) {
        if (y < scrollFirst || y > scrollLast) {
            return;
        }
        shiftRegion(y, scrollLast, -distance(parameters.get(0), HEIGHT));
        setCursorPos(0, y);
    }

    private void SU(final TerminalParser.Parameters parameters) {
        shiftRegion(scrollFirst, scrollLast, -distance(parameters.get(0), HEIGHT));
    }

    private void SD(final TerminalParser.Parameters parameters) {
        shiftRegion(scrollFirst, scrollLast, distance(parameters.get(0), HEIGHT));
    }

    private void SGR(final TerminalParser.Parameters parameters) {
        for (int i = 0; i < parameters.count(); i++) {
            final int sgr = parameters.get(i);
            if (sgr == SGR_FOREGROUND_EXTENDED || sgr == SGR_BACKGROUND_EXTENDED) {
                i = selectExtendedColor(i, parameters);
            } else {
                selectStyle(sgr);
            }
        }
    }

    private void EL(final TerminalParser.Parameters parameters) {
        switch (parameters.get(0)) {
            case 0 ->  // From cursor to end of line
                clearLine(y, x, WIDTH);
            case 1 ->  // From beginning of line to cursor
                clearLine(y, 0, x + 1);
            case 2 ->  // Entire line containing cursor
                clearLine(y);
        }
    }

    private void ED(final TerminalParser.Parameters parameters) {
        switch (parameters.get(0)) {
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

    private void DECSTBM(final TerminalParser.Parameters parameters) {
        final int first = parameters.get(0) > 0 ? parameters.get(0) - 1 : 0;
        final int requested = parameters.count() > 1 && parameters.get(1) > 0 ? parameters.get(1) - 1 : HEIGHT - 1;
        final int last = Math.min(requested, HEIGHT - 1);
        if (first < last) {
            scrollFirst = first;
            scrollLast = last;
        } else { // A region that is not at least two lines tall means the whole screen.
            scrollFirst = 0;
            scrollLast = HEIGHT - 1;
        }
        setRelativeCursorPos(0, 0); // send cursor home
    }

    private void TBC(final TerminalParser.Parameters parameters) {
        switch (parameters.get(0)) {
            case 0 -> { // Clear tab at current column
                if (x >= 0 && x < WIDTH) {
                    tabs[x] = false;
                }
            }
            case 3 -> // Clear all tabs
                Arrays.fill(tabs, false);
        }
    }

    private void SM(final TerminalParser.Parameters parameters) {
        for (int i = 0; i < parameters.count(); i++) {
            final int mode = parameters.get(i);
            if (mode != 0) {
                setMode(parameters.isPrivate(), mode);
            }
            applyModeSideEffects(parameters.isPrivate(), mode);
        }
    }

    private void RM(final TerminalParser.Parameters parameters) {
        for (int i = 0; i < parameters.count(); i++) {
            final int mode = parameters.get(i);
            if (mode != 0) {
                resetMode(parameters.isPrivate(), mode);
            }
            applyModeSideEffects(parameters.isPrivate(), mode);
        }
    }

    private void DSR(final TerminalParser.Parameters parameters) {
        switch (parameters.get(0)) {
            case 5 -> // Report console status
                putResponse("\033[0n"); // Ready, No malfunctions detected
            case 6 -> { // Report cursor position
                final int row = getPrivateMode(Mode.DECOM) ? y - scrollFirst + 1 : y + 1;
                putResponse(String.format(Locale.ROOT, "\033[%d;%dR", row, x + 1));
            }
        }
    }

    private void DECCOLM() {
        scrollFirst = 0;
        scrollLast = HEIGHT - 1;
        isWrapPending = false;
        setCursorPos(0, 0);
        clear();
    }

    private void DA() {
        putResponse("\033[?1;0c"); // No options.
    }

    private static int distance(final int argument, final int limit) {
        return Math.clamp(argument, 1, limit);
    }

    private static boolean isGraphicsSet(final char set) {
        return set == '0' || set == '2';
    }

    private void saveCursor(final SavedCursor savedCursor) {
        savedCursor.x = x;
        savedCursor.y = y;
        savedCursor.color = color;
        savedCursor.style = style;
        savedCursor.isG0Graphics = isG0Graphics;
        savedCursor.isG1Graphics = isG1Graphics;
        savedCursor.isShiftedOut = isShiftedOut;
        savedCursor.isOriginMode = getPrivateMode(Mode.DECOM);
        savedCursor.isWrapPending = isWrapPending;
    }

    private void restoreCursor(final SavedCursor savedCursor) {
        color = savedCursor.color;
        style = savedCursor.style;
        isG0Graphics = savedCursor.isG0Graphics;
        isG1Graphics = savedCursor.isG1Graphics;
        isShiftedOut = savedCursor.isShiftedOut;
        if (savedCursor.isOriginMode) {
            setMode(true, Mode.DECOM);
        } else {
            resetMode(true, Mode.DECOM);
        }
        setCursorPos(savedCursor.x, savedCursor.y);
        isWrapPending = savedCursor.isWrapPending; // Placing the cursor cleared it.
    }

    private int clampToLineEnd(final TerminalParser.Parameters parameters) {
        return Math.min(distance(parameters.get(0), WIDTH), WIDTH - x);
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

    private void applyModeSideEffects(final boolean isPrivate, final int mode) {
        if (!isPrivate) {
            return;
        }
        switch (mode) {
            case Mode.DECOM -> setRelativeCursorPos(0, 0);
            case Mode.DECCOLM -> DECCOLM();
            case Mode.ALT_BUFFER, Mode.ALT_BUFFER_CLEAR, Mode.ALT_BUFFER_AND_CURSOR -> updateAltBuffer();
            case Mode.SAVE_CURSOR -> {
                if (getPrivateMode(mode)) {
                    DECSC();
                } else {
                    DECRC();
                }
            }
            default -> {
            }
        }
    }

    private void setMode(final boolean isPrivate, final int mode) {
        if (isPrivate && mode >= Integer.SIZE) {
            final int bit = highPrivateModeBitIndex(mode);
            if (bit >= 0) {
                highPrivateModes |= 1 << bit;
            }
        } else if (mode < Integer.SIZE) {
            if (isPrivate) {
                privateModes |= 1 << mode;
            } else {
                modes |= 1 << mode;
            }
        }
    }

    private void resetMode(final boolean isPrivate, final int mode) {
        if (isPrivate && mode >= Integer.SIZE) {
            final int bit = highPrivateModeBitIndex(mode);
            if (bit >= 0) {
                highPrivateModes &= ~(1 << bit);
            }
        } else if (mode < Integer.SIZE) {
            if (isPrivate) {
                privateModes &= ~(1 << mode);
            } else {
                modes &= ~(1 << mode);
            }
        }
    }

    private static int highPrivateModeBitIndex(final int mode) {
        return switch (mode) {
            case Mode.BRACKETED_PASTE -> 0;
            case Mode.ALT_BUFFER_CLEAR -> 1;
            case Mode.ALT_BUFFER -> 2;
            case Mode.SAVE_CURSOR -> 3;
            case Mode.ALT_BUFFER_AND_CURSOR -> 4;
            case Mode.MOUSE_TRACKING -> 5;
            case Mode.MOUSE_SGR -> 6;
            case Mode.ALTERNATE_SCROLL -> 7;
            default -> -1;
        };
    }

    private void updateAltBuffer() {
        final boolean wantsAltScreen = getPrivateMode(Mode.ALT_BUFFER)
            || getPrivateMode(Mode.ALT_BUFFER_CLEAR)
            || getPrivateMode(Mode.ALT_BUFFER_AND_CURSOR);
        boolean hasAltScreen = savedScreen != null;
        if (wantsAltScreen == hasAltScreen) {
            return;
        }

        if (wantsAltScreen) {
            final var screen = new SavedScreen();
            screen.buffer = buffer.clone();
            screen.colors = colors.clone();
            screen.styles = styles.clone();
            if (getPrivateMode(Mode.ALT_BUFFER_AND_CURSOR)) {
                screen.cursor = new SavedCursor();
                saveCursor(screen.cursor);
            }
            savedScreen = screen;
            clear();
        } else {
            final var screen = savedScreen;
            savedScreen = null;
            System.arraycopy(screen.buffer, 0, buffer, 0, buffer.length);
            System.arraycopy(screen.colors, 0, colors, 0, colors.length);
            System.arraycopy(screen.styles, 0, styles, 0, styles.length);
            if (screen.cursor != null) {
                restoreCursor(screen.cursor);
            }
            listeners.forEach(Listener::handleTerminalChanged);
        }
    }

    private boolean getMode(final int mode) {
        return (modes & (1 << mode)) != 0;
    }

    private boolean getPrivateMode(final int mode) {
        if (mode < Integer.SIZE) {
            return (privateModes & (1 << mode)) != 0;
        }
        final int bit = highPrivateModeBitIndex(mode);
        return bit >= 0 && (highPrivateModes & (1 << bit)) != 0;
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
                setColor(true, sgr - 30, false);
            case 39 -> // Default foreground color
                setColor(true, Color.WHITE, false);
            case 40, 41, 42, 43, 44, 45, 46, 47 -> //–47 Set background color
                setColor(false, sgr - 40, false);
            case 49 -> // Default background color
                setColor(false, Color.BLACK, false);
            case 90, 91, 92, 93, 94, 95, 96, 97 -> // Set bright foreground color
                setColor(true, sgr - 90, true);
            case 100, 101, 102, 103, 104, 105, 106, 107 -> // Set bright background color
                setColor(false, sgr - 100, true);
        }
    }

    private int selectExtendedColor(final int index, final TerminalParser.Parameters parameters) {
        if (index + 1 >= parameters.count()) {
            return index;
        }

        final boolean isForeground = parameters.get(index) == SGR_FOREGROUND_EXTENDED;
        switch (parameters.get(index + 1)) {
            case 5 -> { // ESC[38;5;n – 256 color palette
                if (index + 2 >= parameters.count()) {
                    return parameters.count();
                }
                setColor(isForeground, paletteToColorIndex(parameters.get(index + 2)), paletteIsBright(parameters.get(index + 2)));
                return index + 2;
            }
            case 2 -> { // ESC[38;2;r;g;b – 24 bit color
                if (index + 4 >= parameters.count()) {
                    return parameters.count();
                }
                setColor(isForeground, rgbToColorIndex(parameters.get(index + 2), parameters.get(index + 3), parameters.get(index + 4)), false);
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

    private static boolean paletteIsBright(final int palette) {
        return palette >= 8 && palette < 16;
    }

    private static int rgbToColorIndex(final int r, final int g, final int b) {
        return channelToColorMask(r, 128, Color.RED)
            | channelToColorMask(g, 128, Color.GREEN)
            | channelToColorMask(b, 128, Color.BLUE);
    }

    private static int channelToColorMask(final int value, final int threshold, final int mask) {
        return value >= threshold ? mask : 0;
    }

    private void setColor(final boolean isForeground, final int value, final boolean isBright) {
        if (isForeground) {
            color = (byte) ((color & ~((COLOR_MASK << COLOR_FOREGROUND_SHIFT) | COLOR_FOREGROUND_BRIGHT_MASK))
                | (value << COLOR_FOREGROUND_SHIFT) | (isBright ? COLOR_FOREGROUND_BRIGHT_MASK : 0));
        } else {
            color = (byte) ((color & ~COLOR_BACKGROUND_MASK)
                | value | (isBright ? COLOR_BACKGROUND_BRIGHT_MASK : 0));
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
        return (byte) ((DEFAULT_COLORS & ~COLOR_BACKGROUND_MASK) | (color & COLOR_BACKGROUND_MASK));
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

    private void shiftCells(final int count) {
        final int rowIndex = y * WIDTH;
        final int moved = WIDTH - x - Math.abs(count);
        if (count > 0) {
            if (moved > 0) {
                System.arraycopy(buffer, rowIndex + x, buffer, rowIndex + x + count, moved);
                System.arraycopy(colors, rowIndex + x, colors, rowIndex + x + count, moved);
                System.arraycopy(styles, rowIndex + x, styles, rowIndex + x + count, moved);
            }
            clearLine(y, x, x + count);
        } else if (count < 0) {
            if (moved > 0) {
                System.arraycopy(buffer, rowIndex + x - count, buffer, rowIndex + x, moved);
                System.arraycopy(colors, rowIndex + x - count, colors, rowIndex + x, moved);
                System.arraycopy(styles, rowIndex + x - count, styles, rowIndex + x, moved);
            }
            clearLine(y, WIDTH + count, WIDTH);
        }
    }

    private void shiftRegion(final int firstLine, final int lastLine, final int count) {
        if (Math.abs(count) >= lastLine - firstLine + 1) {
            for (int iy = firstLine; iy <= lastLine; iy++) {
                clearLine(iy);
            }
        } else if (count > 0) {
            shiftLines(firstLine, lastLine - count, count);
        } else if (count < 0) {
            shiftLines(firstLine - count, lastLine, count);
        }
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
